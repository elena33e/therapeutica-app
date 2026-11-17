package com.therapeutica.therapeutica_app.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.api.FlywayException;
import java.sql.SQLException;

import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class SimplificareSchemaTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.1")
                    .withDatabaseName("therapeutica_migration_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    private Flyway flywayPanaLa(String versiune) {
        return Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .target(versiune)
                .cleanDisabled(true)
                .load();
    }

    @Test
    void simplificaSchemaPastrandDocumenteleSiRetragerile() throws Exception {
        flywayPanaLa("1").migrate();

        Map<UUID, String> statusuriAsteptate = new LinkedHashMap<>();
        Map<UUID, String> caiAsteptate = new LinkedHashMap<>();

        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {

            UUID medicId = UUID.randomUUID();
            UUID utilizatorPacientId = UUID.randomUUID();
            UUID pacientId = UUID.randomUUID();

            try (var insert = connection.prepareStatement("""
                    INSERT INTO public.utilizatori
                        (id, created_at, email, nume, prenume, rol, parola)
                    VALUES (?, CURRENT_TIMESTAMP, ?, 'Test', 'Test', ?, 'fictiv')
                    """)) {
                insert.setObject(1, medicId);
                insert.setString(2, "medic@example.test");
                insert.setString(3, "MEDIC");
                insert.executeUpdate();

                insert.setObject(1, utilizatorPacientId);
                insert.setString(2, "pacient@example.test");
                insert.setString(3, "PACIENT");
                insert.executeUpdate();
            }

            try (var insert = connection.prepareStatement("""
                    INSERT INTO public.medici (user_id, specializare)
                    VALUES (?, 'Test')
                    """)) {
                insert.setObject(1, medicId);
                insert.executeUpdate();
            }

            try (var insert = connection.prepareStatement("""
                    INSERT INTO public.pacienti (id, user_id, medic_id)
                    VALUES (?, ?, ?)
                    """)) {
                insert.setObject(1, pacientId);
                insert.setObject(2, utilizatorPacientId);
                insert.setObject(3, medicId);
                insert.executeUpdate();
            }

            String[] statusuriVechi = {
                    "INCARCAT", "VALIDAT", "PROCESAT", "STANDARDIZAT",
                    "INTERPRETAT", "FHIR_GENERAT", "STERS_DE_PACIENT"
            };

            try (var insert = connection.prepareStatement("""
                    INSERT INTO public.documente_medicale
                        (id, pacient_id, cale_fisier_stocare, status)
                    VALUES (?, ?, ?, ?)
                    """)) {
                for (String status : statusuriVechi) {
                    UUID documentId = UUID.randomUUID();
                    String cale = "/documente-test/" + documentId;

                    insert.setObject(1, documentId);
                    insert.setObject(2, pacientId);
                    insert.setString(3, cale);
                    insert.setString(4, status);
                    insert.executeUpdate();

                    caiAsteptate.put(documentId, cale);
                    statusuriAsteptate.put(
                            documentId,
                            status.equals("STERS_DE_PACIENT")
                                    ? "STERS_DE_PACIENT"
                                    : "INCARCAT"
                    );
                }
            }
        }

        var flyway = flywayPanaLa("2");

// Introducem un document care necesită verificare manuală.
        UUID documentCuEroareId = UUID.randomUUID();

        try (
                var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                );
                var insert = connection.prepareStatement("""
                INSERT INTO public.documente_medicale
                    (id, pacient_id, cale_fisier_stocare, status)
                SELECT ?, pacient_id, '/documente-test/eroare', 'EROARE'
                FROM public.documente_medicale
                LIMIT 1
                """)
        ) {
            insert.setObject(1, documentCuEroareId);
            assertEquals(1, insert.executeUpdate());
        }

// Migrarea trebuie refuzată din motivul așteptat.
        var exceptie = assertThrows(FlywayException.class, () -> flyway.migrate());

        Throwable cauza = exceptie;
        while (cauza.getCause() != null) {
            cauza = cauza.getCause();
        }

        var eroareSql = assertInstanceOf(SQLException.class, cauza);
        assertEquals("P0001", eroareSql.getSQLState());
        assertTrue(eroareSql.getMessage().contains(
                "Migrarea necesită verificarea lor."
        ));

// V1 trebuie să rămână ultima migrare aplicată.
        assertEquals("1", flyway.info().current().getVersion().toString());

        try (
                var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                );
                var statement = connection.createStatement()
        ) {
            // Documentele și statusurile de procesare trebuie să fie păstrate.
            try (var result = statement.executeQuery("""
            SELECT
                COUNT(*),
                COUNT(*) FILTER (
                    WHERE status IN (
                        'VALIDAT', 'PROCESAT', 'STANDARDIZAT',
                        'INTERPRETAT', 'FHIR_GENERAT'
                    )
                ),
                COUNT(*) FILTER (WHERE status = 'EROARE')
            FROM public.documente_medicale
            """)) {
                assertTrue(result.next());
                assertEquals(8, result.getInt(1));
                assertEquals(5, result.getInt(2));
                assertEquals(1, result.getInt(3));
            }

            // Structurile vechi nu trebuie să fi fost eliminate.
            try (var result = statement.executeQuery("""
            SELECT
                to_regclass('public.istoric_diagnoze'),
                EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'documente_medicale'
                      AND column_name = 'date_brute'
                )
            """)) {
                assertTrue(result.next());
                assertNotNull(result.getString(1));
                assertTrue(result.getBoolean(2));
            }

            // Eliminăm doar documentul fictiv introdus pentru acest scenariu.
            try (var delete = connection.prepareStatement("""
            DELETE FROM public.documente_medicale WHERE id = ?
            """)) {
                delete.setObject(1, documentCuEroareId);
                assertEquals(1, delete.executeUpdate());
            }
        }

// După eliminarea cauzei, migrarea trebuie să reușească.
        assertEquals(1, flyway.migrate().migrationsExecuted);
        flyway.validate();

        try (
                var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                );
                var statement = connection.createStatement()
        ) {
            int documenteGasite = 0;

            try (var result = statement.executeQuery("""
                    SELECT id, status, cale_fisier_stocare
                    FROM public.documente_medicale
                    """)) {
                while (result.next()) {
                    UUID id = result.getObject("id", UUID.class);

                    assertTrue(statusuriAsteptate.containsKey(id));
                    assertEquals(
                            statusuriAsteptate.get(id),
                            result.getString("status")
                    );
                    assertEquals(
                            caiAsteptate.get(id),
                            result.getString("cale_fisier_stocare")
                    );
                    documenteGasite++;
                }
            }

            assertEquals(statusuriAsteptate.size(), documenteGasite);

            try (var result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND (
                          (table_name = 'documente_medicale'
                           AND column_name IN (
                               'date_brute', 'date_validate',
                               'date_standardizate_loinc',
                               'date_interpretate_fhir',
                               'candidati_loinc', 'date_fhir_standard'
                           ))
                          OR
                          (table_name = 'intrebari'
                           AND column_name IN (
                               'hpo_code', 'hpo_term',
                               'hpo_trigger_code', 'hpo_trigger_term'
                           ))
                      )
                    """)) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }

            try (var result = statement.executeQuery(
                    "SELECT to_regclass('public.istoric_diagnoze')")) {
                assertTrue(result.next());
                assertNull(result.getString(1));
            }
        }

        assertEquals(0, flyway.migrate().migrationsExecuted);
    }
}