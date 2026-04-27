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

    /**
     * Listează chestionarele disponibile pentru un pacient
     */
    @GetMapping("/pacient/{userId}/disponibile")
    public String listeazaDisponibile(@PathVariable UUID userId, Model model) {

        log.info("[RaspunsuriChestionare] Afișare chestionare pentru USER: {}", userId);

        try {
            Optional<Pacienti> pacientOpt = pacientiRepository.findByUserId(userId);

            if (pacientOpt.isEmpty()) {
                log.warn("Pacient not found for user: {}", userId);
                return setupEmptyModel(model, userId, "disponibile",
                        "Nu s-a găsit profil de pacient pentru contul tău.");
            }

            Pacienti pacient = pacientOpt.get();
            UUID pacientId = pacient.getId();
            log.info("Converted user {} to pacient {}", userId, pacientId);

            // Folosește metoda cu toate relațiile pentru chestionarele NECOMPLETAT
            List<RaspunsuriChestionare> chestionareDisponibile = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(pacientId,
                            RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);

            // Pentru istoric (COMPLETAT)
            List<RaspunsuriChestionare> istoricChestionare = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(pacientId,
                            RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

            log.info("Found {} chestionare disponibile, {} în istoric",
                    chestionareDisponibile.size(), istoricChestionare.size());

            // DEBUG: Verifică datele încărcate
            debugLogChestionare("Disponibile", chestionareDisponibile);
            debugLogChestionare("Istoric", istoricChestionare);

            model.addAttribute("chestionareDisponibile", chestionareDisponibile);
            model.addAttribute("istoricChestionare", istoricChestionare);
            model.addAttribute("pacientId", pacientId);
            model.addAttribute("userId", userId);
            model.addAttribute("activeTab", "disponibile");

            return "pacient/chestionare";

        } catch (Exception e) {
            log.error("Eroare la încărcarea chestionarelor: {}", e.getMessage(), e);
            return setupEmptyModel(model, userId, "disponibile",
                    "Eroare la încărcarea datelor: " + e.getMessage());
        }
    }

    /**
     * Istoricul chestionarelor completate de un pacient
     */
    @GetMapping("/pacient/{userId}/istoric")
    public String afiseazaIstoric(@PathVariable UUID userId, Model model) {

        log.info("[RaspunsuriChestionare] Afișare istoric pentru USER: {}", userId);

        try {
            Optional<Pacienti> pacientOpt = pacientiRepository.findByUserId(userId);

            if (pacientOpt.isEmpty()) {
                log.warn("Pacient not found for user: {}", userId);
                return setupEmptyModel(model, userId, "istoric",
                        "Nu s-a găsit profil de pacient pentru contul tău.");
            }

            Pacienti pacient = pacientOpt.get();
            UUID pacientId = pacient.getId();

            // Folosește metoda cu toate relațiile pentru chestionarele COMPLETAT
            List<RaspunsuriChestionare> istoric = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(pacientId,
                            RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

            // Chestionare disponibile pentru tab-uri
            List<RaspunsuriChestionare> chestionareDisponibile = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(pacientId,
                            RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);

            log.info("Found {} chestionare in istoric, {} disponibile",
                    istoric.size(), chestionareDisponibile.size());

            // DEBUG
            debugLogChestionare("Istoric", istoric);

            model.addAttribute("istoricChestionare", istoric);
            model.addAttribute("chestionareDisponibile", chestionareDisponibile);
            model.addAttribute("pacientId", pacientId);
            model.addAttribute("userId", userId);  // ← IMPORTANT pentru template!
            model.addAttribute("activeTab", "istoric");

            return "pacient/chestionare";

        } catch (Exception e) {
            log.error("❌ Eroare la încărcarea istoricului: {}", e.getMessage(), e);
            return setupEmptyModel(model, userId, "istoric",
                    "Eroare la încărcarea istoricului: " + e.getMessage());
        }
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