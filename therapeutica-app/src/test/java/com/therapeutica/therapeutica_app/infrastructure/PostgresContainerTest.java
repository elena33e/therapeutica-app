package com.therapeutica.therapeutica_app.infrastructure;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class PostgresContainerTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.1")
                    .withDatabaseName("therapeutica_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Test
    void permiteConectareaSiExecutareaUneiInterogari() throws Exception {
        try (
                var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                );
                var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT current_database(), 1 + 1"
                )
        ) {
            assertTrue(result.next());
            assertEquals("therapeutica_test", result.getString(1));
            assertEquals(2, result.getInt(2));
        }
    }
}