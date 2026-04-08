package com.therapeutica.therapeutica_app.notificari;


import org.springframework.stereotype.Service;

@Service
public class NotificareService {

    // Comentează injectarea sau folosește @Autowired(required = false)
    // private final JavaMailSender mailSender;

    // Constructor fără JavaMailSender
    public NotificareService() {
        // this.mailSender = mailSender;
    }

    public void trimiteEmail(String destinatar, String subiect, String mesaj) {
        // Pentru moment, doar loghează
        System.out.println("📧 [EMAIL MOCK]");
        System.out.println("   To: " + destinatar);
        System.out.println("   Subject: " + subiect);
        System.out.println("   Message: " + mesaj.substring(0, Math.min(mesaj.length(), 100)) + "...");

        // În viitor, va trimite realmente:
        // if (mailSender != null) {
        //     SimpleMailMessage email = new SimpleMailMessage();
        //     email.setTo(destinatar);
        //     email.setSubject(subiect);
        //     email.setText(mesaj);
        //     mailSender.send(email);
        // }
    }
}