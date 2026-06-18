package com.therapeutica.therapeutica_app.resetare_parola;


import com.therapeutica.therapeutica_app.util.EmailService;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class ResetareParolaService {

    private final TokenResetareRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void genereazaSiTrimiteToken(Utilizatori utilizator) {

        tokenRepository.findByUtilizatorId(utilizator.getId())
                .ifPresent(token -> {
                    tokenRepository.delete(token);
                    // Forțăm Hibernate să execute DELETE imediat în DB
                    tokenRepository.flush();
                });
        // Token nou
        String tokenString = UUID.randomUUID().toString();
        TokenResetare tokenNou = new TokenResetare();
        tokenNou.setToken(tokenString);
        tokenNou.setUtilizator(utilizator);
        tokenNou.setDataExpirare(LocalDateTime.now().plusMinutes(15));

        tokenRepository.save(tokenNou);

        // Trimitre mail
        String linkResetare = "http://localhost:8080/resetare-parola/schimba-view?token=" + tokenNou.getToken();
        emailService.trimiteEmailResetare(utilizator.getEmail(), linkResetare);
    }

    @Transactional
    public boolean reseteazaParola(String token, String parolaNoua) {
        TokenResetare tokenResetare = tokenRepository.findByToken(token)
                .orElse(null);

        if (tokenResetare == null || tokenResetare.getDataExpirare().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Actualizare parola utilizator
        Utilizatori utilizator = tokenResetare.getUtilizator();
        utilizator.setParola(passwordEncoder.encode(parolaNoua));

        // Stergere token
        tokenRepository.delete(tokenResetare);
        return true;
    }

    public boolean esteTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(t -> t.getDataExpirare().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}