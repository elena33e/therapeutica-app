package com.therapeutica.therapeutica_app.notificari.external;

import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppService {

    public String genereazaLinkMesaj(String telefon, String cod) {
        if (telefon == null || telefon.isBlank() || cod == null || cod.isBlank()) {
            throw new IllegalArgumentException("Telefonul și codul sunt obligatorii.");
        }

        String numarCurat = telefon.replaceAll("[^0-9]", "");

        if (numarCurat.isEmpty()) {
            throw new IllegalArgumentException("Numărul de telefon nu conține cifre.");
        }

        // Repară formatul strict pentru numerele de România, ignoră-le pe cele internaționale deja formatate
        if (numarCurat.startsWith("07") && numarCurat.length() == 10) {
            numarCurat = "4" + numarCurat;
        } else if (!numarCurat.startsWith("40") && !numarCurat.startsWith("0") && numarCurat.length() == 9) {
            numarCurat = "40" + numarCurat;
        }

        String mesaj = "Bună ziua! Codul dumneavoastră de înregistrare pentru portalul medical este: *" + cod + "*";
        String mesajEncodat = URLEncoder.encode(mesaj, StandardCharsets.UTF_8);

        return "https://wa.me/" + numarCurat + "?text=" + mesajEncodat;
    }

    public String genereazaLinkContactPersonalizat(String telefon, String mesajPredefinit) {
        if (telefon == null || telefon.isBlank()) {
            return null;
        }

        String numarCurat = telefon.replaceAll("[^0-9]", "");

        if (numarCurat.isEmpty()) {
            return null;
        }

        if (numarCurat.startsWith("07") && numarCurat.length() == 10) {
            numarCurat = "4" + numarCurat;
        } else if (!numarCurat.startsWith("40") && !numarCurat.startsWith("0") && numarCurat.length() == 9) {
            numarCurat = "40" + numarCurat;
        }

        String mesajEncodat = URLEncoder.encode(mesajPredefinit != null ? mesajPredefinit : "Bună ziua!", StandardCharsets.UTF_8);

        return "https://wa.me/" + numarCurat + "?text=" + mesajEncodat;
    }
}