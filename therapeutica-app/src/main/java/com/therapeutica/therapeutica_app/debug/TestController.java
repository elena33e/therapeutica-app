package com.therapeutica.therapeutica_app.debug;

import com.therapeutica.therapeutica_app.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

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

    @GetMapping("/genereaza-hash/{parola}")
    public String genereazaHash(@PathVariable String parola) {
        String hashNou = passwordEncoder.encode(parola);
        return "Hash-ul perfect pentru parola '" + parola + "' este:<br><br><b>" + hashNou + "</b>";
    }

    // NOUA METODĂ PENTRU TESTAREA HASH-ULUI
    @GetMapping("/test-parola/{email}/{parolaTest}")
    public String testParola(@PathVariable String email, @PathVariable String parolaTest) {
        try {
            UserDetails user = customUserDetailsService.loadUserByUsername(email);
            boolean match = passwordEncoder.matches(parolaTest, user.getPassword());
            return "Hash DB: " + user.getPassword() + "<br>Parola trimisă: " + parolaTest + "<br>Rezultat match: " + match;
        } catch (Exception e) {
            return "Eroare la găsirea utilizatorului: " + e.getMessage();
        }
    }
}