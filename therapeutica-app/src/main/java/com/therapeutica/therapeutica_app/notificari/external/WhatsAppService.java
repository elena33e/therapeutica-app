package com.therapeutica.therapeutica_app.notificari.external;

import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppService {

    public String genereazaLinkMesaj(String telefon, String cod) {
        if (telefon == null || telefon.isEmpty() || cod == null || cod.isEmpty()) {
            throw new IllegalArgumentException("Telefonul și codul sunt obligatorii pentru generarea link-ului.");
        }

        // Curățăm numărul (doar cifre)
        String numarCurat = telefon.replaceAll("[^0-9]", "");

        // Formatere pt Romania
        if (numarCurat.startsWith("07") && numarCurat.length() == 10) {
            numarCurat = "4" + numarCurat;
        }

        // Construim mesajul
        String mesaj = "Bună ziua! Codul dumneavoastră de înregistrare pentru portalul medical este: *" + cod + "*";

        // Encodăm mesajul pentru URL
        String mesajEncodat = URLEncoder.encode(mesaj, StandardCharsets.UTF_8);

        return "https://wa.me/" + numarCurat + "?text=" + mesajEncodat;
    }
}