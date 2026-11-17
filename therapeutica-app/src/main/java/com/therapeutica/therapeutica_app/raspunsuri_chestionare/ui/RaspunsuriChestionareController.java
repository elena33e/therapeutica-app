package com.therapeutica.therapeutica_app.raspunsuri_chestionare.ui;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/raspunsuri-chestionare")
@RequiredArgsConstructor
@Slf4j
public class RaspunsuriChestionareController {

    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final PacientiRepository pacientiRepository;

    @GetMapping("/pacient/{userId}/disponibile")
    public String listeazaDisponibile(@PathVariable UUID userId) {
        return "redirect:/chestionare/pacient/" + userId + "/disponibile";
    }

    @GetMapping("/pacient/{userId}/istoric")
    public String afiseazaIstoric(@PathVariable UUID userId) {
        return "redirect:/chestionare/pacient/" + userId + "/istoric";
    }
    /**
     * Helper method pentru debugging
     */
    private void debugLogChestionare(String tip, List<RaspunsuriChestionare> chestionare) {
        log.info("DEBUG {} ({} chestionare):", tip, chestionare.size());
        for (RaspunsuriChestionare rc : chestionare) {
            String chestionarNume = rc.getChestionar() != null ? rc.getChestionar().getNume() : "NULL";

            String medicNume = "NULL";
            if (rc.getMedic() != null && rc.getMedic().getUser() != null) {
                medicNume = rc.getMedic().getUser().getNume() + " " +
                        rc.getMedic().getUser().getPrenume();
            }

            String pacientNume = "NULL";
            if (rc.getPacient() != null && rc.getPacient().getUser() != null) {
                pacientNume = rc.getPacient().getUser().getNume() + " " +
                        rc.getPacient().getUser().getPrenume();
            }

            log.info("   ├─ ID: {}, Status: {}, Chestionar: {}, Medic: {}, Pacient: {}, Completat: {}",
                    rc.getId(), rc.getStatus(), chestionarNume, medicNume, pacientNume, rc.getCompletatLa());
        }
    }

    /**
     * Helper method pentru setup model cu date goale
     */
    private String setupEmptyModel(Model model, UUID userId, String activeTab, String message) {
        model.addAttribute("chestionareDisponibile", new ArrayList<>());
        model.addAttribute("istoricChestionare", new ArrayList<>());
        model.addAttribute("pacientId", userId);  // Folosește userId ca fallback
        model.addAttribute("userId", userId);
        model.addAttribute("activeTab", activeTab);

        if (message != null && !message.isEmpty()) {
            if (message.contains("Eroare") || message.toLowerCase().contains("error")) {
                model.addAttribute("error", message);
            } else {
                model.addAttribute("info", message);
            }
        }

        return "pacient/chestionare";
    }

    /**
     * Vizualizează detalii despre un răspuns chestionar (pentru pacient)
     */
    @GetMapping("/{raspunsChestionarId}/detalii")
    public String vizualizeazaDetalii(@PathVariable UUID raspunsChestionarId,
                                      Model model) {

        log.info("[RaspunsuriChestionare] Vizualizare detalii pentru: {}", raspunsChestionarId);

        // TODO: Implementare vizualizare detalii
        model.addAttribute("raspunsChestionarId", raspunsChestionarId);
        return "pacient/detalii-chestionar";
    }
}