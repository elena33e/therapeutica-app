package com.therapeutica.therapeutica_app.debug;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/debug-env")
    public String debugEnv() {
        StringBuilder result = new StringBuilder();
        result.append("<h2>Environment Variables:</h2>");

        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        String username = System.getenv("JDBC_DATABASE_USERNAME");
        String password = System.getenv("JDBC_DATABASE_PASSWORD");

        result.append("JDBC_DATABASE_URL: ").append(jdbcUrl != null ? "***SET***" : "NULL").append("<br>");
        result.append("JDBC_DATABASE_USERNAME: ").append(username != null ? "***SET***" : "NULL").append("<br>");
        result.append("JDBC_DATABASE_PASSWORD: ").append(password != null ? "***SET***" : "NULL").append("<br>");

        result.append("<h2>Details (first 50 chars):</h2>");
        result.append("JDBC_URL: ").append(jdbcUrl != null ? jdbcUrl.substring(0, Math.min(50, jdbcUrl.length())) : "NULL").append("<br>");

        return result.toString();
    }
}