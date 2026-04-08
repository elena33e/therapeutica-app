package com.therapeutica.therapeutica_app.medici.ui;


import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raport_chestionar.RezultatScoring;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raport_chestionar.ScoringService;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariRepository;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import jakarta.servlet.http.HttpSession;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/medic")
@RequiredArgsConstructor
@Slf4j
public class MedicEvaluareController {

    private final MediciRepository mediciRepository;
    private final PacientiRepository pacientiRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final ScoringService scoringService;
    private final RaspunsuriIntrebariRepository raspunsuriIntrebariRepository;

    /**
     * PAS 1: Lista pacienților cu chestionare neevaluate
     * Use Case F9: "Pacienți cu chestionare neevalate"
     */
    @GetMapping("/{medicId}/pacienti-neevaluati")
    public String pacientiCuChestionareNeevaluate(@PathVariable UUID medicId,
                                                  HttpSession session,
                                                  Model model) {

        log.info("GET /medic/{}/pacienti-neevaluati", medicId);

        // Verifică autentificare
        String sessionUserId = (String) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(medicId.toString())) {
            return "redirect:/login";
        }

        try {
            // Obține medicul
            Medici medic = mediciRepository.findById(medicId)
                    .orElseThrow(() -> new RuntimeException("Medic not found"));

            // Obține toți pacienții medicului care au chestionare COMPLETATE
            List<Pacienti> pacientiMedic = pacientiRepository.findPacientiByMedicIdWithUser(medicId);

            // Filtrează pacienții care au cel puțin un chestionar COMPLETAT
            List<PacientCuChestionareDTO> pacientiCuChestionare = new ArrayList<>();

            for (Pacienti pacient : pacientiMedic) {
                // Obține chestionarele completate ale pacientului
                List<RaspunsuriChestionare> chestionareCompletate = raspunsuriChestionareRepository
                        .findByPacientIdAndStatus(pacient.getId(),
                                RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

                if (!chestionareCompletate.isEmpty()) {
                    // Obține datele utilizatorului
                    Utilizatori pacientUser = utilizatoriRepository.findById(pacient.getUser().getId())
                            .orElse(null);

                    PacientCuChestionareDTO dto = PacientCuChestionareDTO.builder()
                            .pacientId(pacient.getId())
                            .userId(pacient.getUser().getId())
                            .nume(pacientUser != null ? pacientUser.getNume() : "Necunoscut")
                            .prenume(pacientUser != null ? pacientUser.getPrenume() : "Necunoscut")
                            .email(pacientUser != null ? pacientUser.getEmail() : "")
                            .numarChestionareCompletate(chestionareCompletate.size())
                            .ultimulChestionarCompletat(chestionareCompletate.stream()
                                    .map(RaspunsuriChestionare::getCompletatLa)
                                    .filter(Objects::nonNull)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(null))
                            .build();

                    pacientiCuChestionare.add(dto);
                }
            }

            // Sortează după ultimul chestionar completat (cele mai recente primele)
            pacientiCuChestionare.sort(Comparator
                    .comparing(PacientCuChestionareDTO::getUltimulChestionarCompletat,
                            Comparator.nullsLast(Comparator.reverseOrder())));

            // Adaugă datele în model
            model.addAttribute("medicId", medicId);
            model.addAttribute("pacienti", pacientiCuChestionare);
            model.addAttribute("totalPacienti", pacientiCuChestionare.size());

            log.info("✅ Găsiți {} pacienți cu chestionare completate", pacientiCuChestionare.size());

            return "medic/pacienti-neevaluati";

        } catch (Exception e) {
            log.error("❌ Eroare la obținerea pacienților: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare la încărcarea datelor: " + e.getMessage());
            return "medic/error";
        }
    }

    /**
     * PAS 2: Detalii pacient cu lista chestionarelor
     * Use Case F9: "Afișează panoul cu detaliiile pacientului"
     */
    @GetMapping("/{medicId}/pacient/{pacientId}/chestionare")
    public String detaliiPacientCuChestionare(@PathVariable UUID medicId,
                                              @PathVariable UUID pacientId,
                                              HttpSession session,
                                              Model model) {

        log.info("GET /medic/{}/pacient/{}/chestionare", medicId, pacientId);

        // Verifică autentificare
        String sessionUserId = (String) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(medicId.toString())) {
            return "redirect:/login";
        }

        try {
            // Obține pacientul și utilizatorul
            Pacienti pacient = pacientiRepository.findById(pacientId)
                    .orElseThrow(() -> new RuntimeException("Pacient not found"));

            Utilizatori pacientUser = utilizatoriRepository.findById(pacient.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found for pacient"));

            // Verifică dacă pacientul aparține medicului
            if (pacient.getMedic() == null || !pacient.getMedic().getUserId().equals(medicId)) {
                model.addAttribute("error", "Pacientul nu aparține medicului curent");
                return "medic/error";
            }

            // Obține toate chestionarele pacientului (completate și necompletate)
            List<RaspunsuriChestionare> toateChestionarele = raspunsuriChestionareRepository
                    .findByPacientIdOrderByCompletatLa(pacientId);

            // Separa chestionarele completate de cele necompletate
            List<ChestionarDTO> chestionareCompletate = toateChestionarele.stream()
                    .filter(c -> c.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT)
                    .map(this::mapToChestionarDTO)
                    .collect(Collectors.toList());

            List<ChestionarDTO> chestionareNecompletate = toateChestionarele.stream()
                    .filter(c -> c.getStatus() == RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT)
                    .map(this::mapToChestionarDTO)
                    .collect(Collectors.toList());

            // Adaugă datele în model
            model.addAttribute("medicId", medicId);
            model.addAttribute("pacient", pacient);
            model.addAttribute("pacientUser", pacientUser);
            model.addAttribute("chestionareCompletate", chestionareCompletate);
            model.addAttribute("chestionareNecompletate", chestionareNecompletate);
            model.addAttribute("totalChestionare", toateChestionarele.size());

            log.info("✅ Pacient: {} {}, {} chestionare completate",
                    pacientUser.getNume(), pacientUser.getPrenume(),
                    chestionareCompletate.size());

            return "medic/detalii-pacient-chestionare";

        } catch (Exception e) {
            log.error("❌ Eroare la obținerea detaliilor pacientului: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare: " + e.getMessage());
            return "medic/error";
        }
    }

    /**
     * PAS 3: Vizualizare rezultate chestionar
     * Use Case F9: "Afișează răspunsurile pacientului, scorurile calculate și raportul vizual"
     */
    @GetMapping("/{medicId}/chestionar/{raspunsChestionarId}/rezultate")
    @Transactional(readOnly = true)
    public String vizualizeazaRezultateChestionar(@PathVariable UUID medicId,
                                                  @PathVariable UUID raspunsChestionarId,
                                                  HttpSession session,
                                                  Model model) {

        log.info("GET /medic/{}/chestionar/{}/rezultate", medicId, raspunsChestionarId);

        // Verifică autentificare
        String sessionUserId = (String) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(medicId.toString())) {
            return "redirect:/login";
        }

        try {
            // Obține entitatea principală
            RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                    .findByIdWithChestionar(raspunsChestionarId)
                    .orElseThrow(() -> new RuntimeException("Chestionar not found"));

            // Verifică dacă chestionarul aparține medicului
            if (raspunsChestionar.getMedic() == null || !raspunsChestionar.getMedic().getUserId().equals(medicId)) {
                log.warn("Acțiune neautorizată: Medicul {} a încercat să acceseze chestionarul {}", medicId, raspunsChestionarId);
                model.addAttribute("error", "Nu aveți permisiunea de a vizualiza acest chestionar.");
                return "medic/error";
            }

            // Obține rezultatele scoring
            RezultatScoring rezultatScoring = scoringService.getScoruriCalculate(raspunsChestionarId)
                    .orElseGet(() -> scoringService.calculeazaSiSalveazaScor(raspunsChestionarId));

            List<RaspunsuriIntrebari> toateRaspunsurile = raspunsuriIntrebariRepository
                    .findByRaspunsChestionarIdWithDetails(raspunsChestionarId);

            // Grupăm răspunsurile pe categorii pentru a le afișa în Accordion
            Map<String, List<RaspunsuriIntrebari>> raspunsuriPeCategorii = toateRaspunsurile.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getCategorie().getNume(),
                            LinkedHashMap::new, // Păstrează ordinea logică a categoriilor
                            Collectors.toList()
                    ));

            // --- NOU: Extragem obiectele de categorie pentru a avea acces la flag-ul 'este_evaluabila' ---
            Map<String, CategoriiChestionare> categoriiMap = toateRaspunsurile.stream()
                    .map(RaspunsuriIntrebari::getCategorie)
                    .distinct()
                    .collect(Collectors.toMap(
                            CategoriiChestionare::getNume,
                            c -> c,
                            (existing, replacement) -> existing
                    ));


            // Pregătește modelul pentru Thymeleaf
            Pacienti pacient = raspunsChestionar.getPacient();
            Utilizatori pacientUser = utilizatoriRepository.findById(pacient.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found for pacient"));

            model.addAttribute("raspunsChestionar", raspunsChestionar);
            model.addAttribute("chestionar", raspunsChestionar.getChestionar());
            model.addAttribute("medicId", medicId);
            model.addAttribute("pacient", pacient);
            model.addAttribute("pacientUser", pacientUser);
            model.addAttribute("rezultat", rezultatScoring);
            model.addAttribute("raspunsuriPeCategorii", raspunsuriPeCategorii);
            model.addAttribute("categoriiMap", categoriiMap);

            return "medic/vizualizare-rezultate";

        } catch (Exception e) {
            log.error("Eroare la vizualizarea rezultatelor: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare: " + e.getMessage());
            return "medic/error";
        }
    }

    /**
     * Helper method pentru mapare la DTO
     */
    private ChestionarDTO mapToChestionarDTO(RaspunsuriChestionare raspunsChestionar) {
        return ChestionarDTO.builder()
                .id(raspunsChestionar.getId())
                .numeChestionar(raspunsChestionar.getChestionar() != null ?
                        raspunsChestionar.getChestionar().getNume() : "Necunoscut")
                .descriereChestionar(raspunsChestionar.getChestionar() != null ?
                        raspunsChestionar.getChestionar().getDescriere() : "")
                .status(raspunsChestionar.getStatus())
                .completatLa(raspunsChestionar.getCompletatLa())
                .scorTotal(raspunsChestionar.getScorTotalGeneral())
                .build();
    }

    // DTO classes
    @Data
    @Builder
    private static class PacientCuChestionareDTO {
        private UUID pacientId;
        private UUID userId;
        private String nume;
        private String prenume;
        private String email;
        private Integer numarChestionareCompletate;
        private LocalDateTime ultimulChestionarCompletat;

        public String getNumeComplet() {
            return nume + " " + prenume;
        }

        public String getUltimaActivitateFormatted() {
            if (ultimulChestionarCompletat == null) {
                return "Niciodată";
            }
            return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .format(ultimulChestionarCompletat);
        }
    }

    @Data
    @Builder
    private static class ChestionarDTO {
        private UUID id;
        private String numeChestionar;
        private String descriereChestionar;
        private RaspunsuriChestionare.StatusRaspuns status;
        private LocalDateTime completatLa;
        private BigDecimal scorTotal;

        public String getCompletatLaFormatted() {
            if (completatLa == null) {
                return "-";
            }
            return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .format(completatLa);
        }

        public String getScorTotalFormatted() {
            return scorTotal != null ? scorTotal + "/5" : "-";
        }
    }
}