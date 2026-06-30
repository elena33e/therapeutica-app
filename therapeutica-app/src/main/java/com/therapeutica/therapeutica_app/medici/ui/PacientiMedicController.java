package com.therapeutica.therapeutica_app.medici.ui;

import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedicService;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.dto.RaspunsChestionarDTO;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedic;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/medic")
@RequiredArgsConstructor
@Slf4j
public class PacientiMedicController {

    private final MediciRepository mediciRepository;
    private final PacientiRepository pacientiRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final DiagnosticMedicService diagnosticMedicService;

    @GetMapping("/{medicId}/pacienti")
    public String listaPacienti(@PathVariable UUID medicId,
                                HttpSession session,
                                Model model) {

        log.info("GET /medic/{}/pacienti - Lista generală pacienți", medicId);

        String sessionUserId = (String) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(medicId.toString())) {
            return "redirect:/login";
        }

        try {
            Medici medic = mediciRepository.findById(medicId)
                    .orElseThrow(() -> new RuntimeException("Medic not found"));

            List<Pacienti> pacientiMedic = pacientiRepository
                    .findPacientiByMedicIdWithUser(medicId);

            List<PacientCuStatisticiDTO> pacientiDTO = new ArrayList<>();

            for (Pacienti pacient : pacientiMedic) {
                Utilizatori user = pacient.getUser();
                if (user == null) continue;

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

            model.addAttribute("medicId", medicId);
            model.addAttribute("pacienti", pacientiDTO);
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

        String sessionUserId = (String) session.getAttribute("userId");
        if (sessionUserId == null || !sessionUserId.equals(medicId.toString())) {
            return "redirect:/login";
        }

        try {
            Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                    .orElseThrow(() -> new RuntimeException("Pacient not found"));

            if (pacient.getMedic() == null ||
                    !pacient.getMedic().getUser().getId().equals(medicId)) {
                model.addAttribute("error", "Pacientul nu aparține medicului curent");
                return "medic/error";
            }

            Utilizatori pacientUser = pacient.getUser();
            if (pacientUser == null) {
                throw new RuntimeException("User not found for pacient");
            }

            // 1. Extragem ENTITĂȚILE pentru chestionare din baza de date
            List<RaspunsuriChestionare> completateEntity = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusInFullRelations(
                            pacientId,
                            List.of(RaspunsuriChestionare.StatusRaspuns.COMPLETAT,
                                    RaspunsuriChestionare.StatusRaspuns.REVIZUIT));

            List<RaspunsuriChestionare> necompletateEntity = raspunsuriChestionareRepository
                    .findByPacientIdAndStatusFullRelations(
                            pacientId,
                            RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);

            // 2. Mapăm Entitățile în DTO
            List<RaspunsChestionarDTO> chestionareCompletate = completateEntity.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            List<RaspunsChestionarDTO> chestionareNecompletate = necompletateEntity.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            // 3. Extragem istoricul diagnosticelor și îl sortăm descrescător (cel mai recent primul)
            List<DiagnosticMedic> istoricDiagnostice = diagnosticMedicService.getIstoricDiagnosticPacient(pacientId);
            if (istoricDiagnostice != null && !istoricDiagnostice.isEmpty()) {
                istoricDiagnostice.sort((d1, d2) -> d2.getDataStabilire().compareTo(d1.getDataStabilire()));
            }

            // 4. Calculăm data ultimei activități comparând ultimul chestionar cu ultimul diagnostic
            LocalDateTime ultimaActivitate = !chestionareCompletate.isEmpty()
                    ? chestionareCompletate.get(0).getCompletatLa()
                    : null;

            if (istoricDiagnostice != null && !istoricDiagnostice.isEmpty()) {
                LocalDateTime ultimulDiag = istoricDiagnostice.get(0).getDataStabilire();
                if (ultimaActivitate == null || ultimulDiag.isAfter(ultimaActivitate)) {
                    ultimaActivitate = ultimulDiag;
                }
            }

            // 5. Trimitem datele către UI
            model.addAttribute("medicId", medicId);
            model.addAttribute("pacient", pacient);
            model.addAttribute("pacientUser", pacientUser);
            model.addAttribute("totalChestionareCompletate", chestionareCompletate.size());
            model.addAttribute("totalChestionareNecompletate", chestionareNecompletate.size());
            model.addAttribute("ultimaActivitate", ultimaActivitate);
            model.addAttribute("chestionareCompletate", chestionareCompletate);
            model.addAttribute("chestionareNecompletate", chestionareNecompletate);
            model.addAttribute("istoricDiagnostice", istoricDiagnostice); // Variabila necesară în HTML

            log.info("Detalii pacient: {} {} | Chestionare: {} completate, {} restante | Diagnostice validate: {}",
                    pacientUser.getNume(), pacientUser.getPrenume(),
                    chestionareCompletate.size(), chestionareNecompletate.size(),
                    (istoricDiagnostice != null ? istoricDiagnostice.size() : 0));

            return "medic/pacienti-detalii";

        } catch (Exception e) {
            log.error("Eroare la obținerea detaliilor pacientului: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare: " + e.getMessage());
            return "medic/error";
        }
    }

    /**
     * Metodă privată pentru izolarea logicii de mapare Entitate -> DTO
     */
    private RaspunsChestionarDTO mapToDTO(RaspunsuriChestionare rc) {
        RaspunsChestionarDTO dto = new RaspunsChestionarDTO();
        dto.setId(rc.getId());

        if (rc.getChestionar() != null) {
            dto.setChestionarId(rc.getChestionar().getId());
            dto.setNumeChestionar(rc.getChestionar().getNume());
        }

        if (rc.getStatus() != null) {
            dto.setStatus(rc.getStatus().name());
        }

        dto.setCompletatLa(rc.getCompletatLa());
        dto.setScorTotalGeneral(rc.getScorTotalGeneral());
        return dto;
    }

    // DTO simplu pentru tabelul general de pacienți (Rămâne neschimbat)
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
}