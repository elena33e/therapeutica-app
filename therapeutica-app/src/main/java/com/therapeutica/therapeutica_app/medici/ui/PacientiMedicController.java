package com.therapeutica.therapeutica_app.medici.ui;

import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import jakarta.servlet.http.HttpSession;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/medic")
@RequiredArgsConstructor
@Slf4j
public class PacientiMedicController {

    private final MediciRepository mediciRepository;
    private final PacientiRepository pacientiRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;

    @GetMapping("/{medicId}/pacienti")
    public String listaPacienti(@PathVariable UUID medicId,
                                HttpSession session,
                                Model model) {

        log.info("GET /medic/{}/pacienti - Lista generală pacienți", medicId);

        // Verificare autentificare
        String sessionUserId = (String) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(medicId.toString())) {
            return "redirect:/login";
        }

        try {
            // Obține medicul
            Medici medic = mediciRepository.findById(medicId)
                    .orElseThrow(() -> new RuntimeException("Medic not found"));

            // Obține toți pacienții medicului CU datele utilizatorului
            List<Pacienti> pacientiMedic = pacientiRepository
                    .findPacientiByMedicIdWithUser(medicId);

            // Transformă în DTO-uri cu statistici simple
            List<PacientCuStatisticiDTO> pacientiDTO = new ArrayList<>();

            for (Pacienti pacient : pacientiMedic) {
                Utilizatori user = pacient.getUser();
                if (user == null) continue;

                // Obține statistici SIMPLE (doar numărătoare)
                long chestionareCompletate = raspunsuriChestionareRepository
                        .findByPacientIdAndStatusFullRelations(
                                pacient.getId(),
                                RaspunsuriChestionare.StatusRaspuns.COMPLETAT)
                        .size();

                long chestionareNecompletate = raspunsuriChestionareRepository
                        .findByPacientIdAndStatusFullRelations(
                                pacient.getId(),
                                RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT)
                        .size();

                PacientCuStatisticiDTO dto = PacientCuStatisticiDTO.builder()
                        .pacient(pacient)
                        .user(user)
                        .numarChestionareCompletate((int) chestionareCompletate)
                        .numarChestionareNecompletate((int) chestionareNecompletate)
                        .build();

                pacientiDTO.add(dto);
            }

            // Adaugă datele în model
            model.addAttribute("medicId", medicId);
            model.addAttribute("pacienti", pacientiDTO); // ATRIBUTUL CRUCIAL!
            model.addAttribute("medic", medic);
            model.addAttribute("totalPacienti", pacientiDTO.size());

            log.info("✅ Lista pacienților: {} pacienți", pacientiDTO.size());

            return "medic/pacienti-list";

        } catch (Exception e) {
            log.error("❌ Eroare la obținerea listei pacienților: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare la încărcarea listei pacienților: " + e.getMessage());
            return "medic/error";
        }
    }

    @GetMapping("/{medicId}/pacient/{pacientId}")
    public String detaliiPacient(@PathVariable UUID medicId,
                                 @PathVariable UUID pacientId,
                                 HttpSession session,
                                 Model model) {

        log.info("GET /medic/{}/pacient/{} - Detalii generale pacient", medicId, pacientId);

        // Verificare autentificare
        String sessionUserId = (String) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(medicId.toString())) {
            return "redirect:/login";
        }

        try {
            // Obține pacientul CU toate relațiile
            Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                    .orElseThrow(() -> new RuntimeException("Pacient not found"));

            // Verifică dacă pacientul aparține medicului
            if (pacient.getMedic() == null ||
                    !pacient.getMedic().getUser().getId().equals(medicId)) {
                model.addAttribute("error", "Pacientul nu aparține medicului curent");
                return "medic/error";
            }

            Utilizatori pacientUser = pacient.getUser();
            if (pacientUser == null) {
                throw new RuntimeException("User not found for pacient");
            }

            // Obține statistici SIMPLE
            long totalChestionareCompletate = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(
                            pacientId,
                            RaspunsuriChestionare.StatusRaspuns.COMPLETAT)
                    .size();

            long totalChestionareNecompletate = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(
                            pacientId,
                            RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT)
                    .size();

            // Ultimul chestionar completat
            List<RaspunsuriChestionare> chestionareCompletate = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(
                            pacientId,
                            RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

            LocalDateTime ultimaActivitate = null;
            if (!chestionareCompletate.isEmpty()) {
                ultimaActivitate = chestionareCompletate.get(0).getCompletatLa();
            }

            // ADAUGĂ DOAR LISTA SIMPLĂ DE CHESTIONARE COMPLETATE (pentru tabel)
            List<ChestionarSimpluDTO> chestionareCompletateLista = new ArrayList<>();
            for (RaspunsuriChestionare rc : chestionareCompletate) {
                if (rc.getChestionar() != null && rc.getCompletatLa() != null) {
                    chestionareCompletateLista.add(
                            ChestionarSimpluDTO.builder()
                                    .id(rc.getId())
                                    .nume(rc.getChestionar().getNume())
                                    .dataCompletare(rc.getCompletatLa())
                                    //.scor(rc.getScorTotal()) // sau ce metodă ai tu pentru scor
                                    .build()
                    );
                }
            }

            // Adaugă datele în model (DOAR CE E NECESAR)
            model.addAttribute("medicId", medicId);
            model.addAttribute("pacient", pacient);
            model.addAttribute("pacientUser", pacientUser);
            model.addAttribute("totalChestionareCompletate", totalChestionareCompletate);
            model.addAttribute("totalChestionareNecompletate", totalChestionareNecompletate);
            model.addAttribute("ultimaActivitate", ultimaActivitate);
            model.addAttribute("chestionareCompletate", chestionareCompletateLista);

            log.info("✅ Detalii pacient: {} {}",
                    pacientUser.getNume(), pacientUser.getPrenume());

            return "medic/pacienti-detalii";

        } catch (Exception e) {
            log.error("❌ Eroare la obținerea detaliilor pacientului: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare: " + e.getMessage());
            return "medic/error";
        }
    }

    // DTO simplu pentru pacient cu statistici
    @Data
    @Builder
    private static class PacientCuStatisticiDTO {
        private Pacienti pacient;
        private Utilizatori user;
        private Integer numarChestionareCompletate;
        private Integer numarChestionareNecompletate;

        public String getNumeComplet() {
            return user != null ? user.getNume() + " " + user.getPrenume() : "Necunoscut";
        }

        public String getEmail() {
            return user != null ? user.getEmail() : "";
        }

        public String getCnp() {
            return pacient != null && pacient.getCnp() != null ? pacient.getCnp() : "-";
        }

        public UUID getPacientId() {
            return pacient != null ? pacient.getId() : null;
        }

        public UUID getUserId() {
            return user != null ? user.getId() : null;
        }
    }

    // DTO simplu pentru chestionare (doar ceea ce trebuie)
    @Data
    @Builder
    private static class ChestionarSimpluDTO {
        private UUID id;
        private String nume;
        private LocalDateTime dataCompletare;
        private Integer scor;
    }
}