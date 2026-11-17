package com.therapeutica.therapeutica_app.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class SchemaMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.1")
                    .withDatabaseName("therapeutica_migration_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Test
    void creeazaSchemaInitialaSiNuReexecutaMigratia() throws Exception {
        var flyway = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .target("1")
                .cleanDisabled(true)
                .load();

        var primaExecutie = flyway.migrate();

        assertEquals(1, primaExecutie.migrationsExecuted);
        assertEquals("1", flyway.info().current().getVersion().toString());

        flyway.validate();

        try (
                var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                );
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT
                            to_regclass('public.utilizatori'),
                            to_regclass('public.raspunsuri_chestionare'),
                            to_regclass('public.raspunsuri_intrebari'),
                            to_regclass('public.documente_medicale')
                        """)
        ) {
            assertTrue(result.next());

            for (int coloana = 1; coloana <= 4; coloana++) {
                assertNotNull(
                        result.getString(coloana),
                        "Lipsește unul dintre tabelele principale"
                );
            }
        }

        var aDouaExecutie = flyway.migrate();

        assertEquals(0, aDouaExecutie.migrationsExecuted);
    }
}