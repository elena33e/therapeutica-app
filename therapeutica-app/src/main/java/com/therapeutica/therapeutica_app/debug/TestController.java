package com.therapeutica.therapeutica_app.debug;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/db-connection")
    public String testDatabaseConnection() {
        try {
            String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
            return "Database connection successful! Result: " + result;
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Application is running!";
    }
}
