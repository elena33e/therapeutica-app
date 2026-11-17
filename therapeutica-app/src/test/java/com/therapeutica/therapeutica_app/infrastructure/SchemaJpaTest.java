package com.therapeutica.therapeutica_app.infrastructure;

import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.intrebari.IntrebariRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.config.location=classpath:/application-schema-test.yml"
})
@ActiveProfiles("schema-test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Testcontainers
class SchemaJpaTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.1")
                    .withDatabaseName("therapeutica_jpa_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Autowired
    private DocumentMedicalRepository documenteRepository;

    @Autowired
    private IntrebariRepository intrebariRepository;

    @Autowired
    private Flyway flyway;

    @Test
    void entitatileSiRepositoryurileFunctioneazaPeSchemaMigrata() {
        assertNotNull(flyway.info().current());
        assertTrue(flyway.info().current().getVersion().isAtLeast("2"));
        assertEquals(0, flyway.info().pending().length);

        flyway.validate();

        assertTrue(documenteRepository.findAll().isEmpty());
        assertTrue(intrebariRepository.findAll().isEmpty());
    }
}