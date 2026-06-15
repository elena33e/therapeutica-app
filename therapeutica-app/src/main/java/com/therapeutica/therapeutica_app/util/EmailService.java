package com.therapeutica.therapeutica_app.util;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void trimiteEmailResetare(String destinatar, String linkResetare) {
        SimpleMailMessage mesaj = new SimpleMailMessage();
        mesaj.setTo(destinatar);
        mesaj.setSubject("Resetare parolă - Therapeutica");
        mesaj.setText("Pentru a reseta parola, accesează link-ul: " + linkResetare +
                "\nAcest link expiră în 15 minute.");

        mailSender.send(mesaj);
    }
}