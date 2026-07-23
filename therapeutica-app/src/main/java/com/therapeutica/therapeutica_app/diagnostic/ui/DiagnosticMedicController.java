package com.therapeutica.therapeutica_app.diagnostic.ui;

import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedic;
import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedicService;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticMedicController {

    private final DiagnosticMedicService diagnosticMedicService;
    private final PacientiRepository pacientiRepository;
    private final AuditService auditService;

    @PostMapping("/salveaza")
    public String salveazaDiagnostic(
            @RequestParam UUID pacientId,
            @RequestParam String diagnosticFinal,
            @RequestParam(required = false) String observatiiClinice,
            @RequestParam(required = false) List<UUID> chestionareIds,
            @RequestParam(required = false) List<UUID> analizeIds,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        log.info("Medic salvează diagnostic pentru pacientul: {}", pacientId);

        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId).orElse(null);

        if (pacient == null || pacient.getMedic() == null || pacient.getMedic().getUser() == null) {
            log.error("Pacientul {} nu a fost găsit sau nu are un medic asociat.", pacientId);
            redirectAttributes.addFlashAttribute("error",
                    "Pacientul nu a fost găsit sau nu are un medic asociat.");
            return "redirect:/medic/analize/dosar/" + pacientId;
        }

        UUID medicUserId = pacient.getMedic().getUser().getId();

        try {
            DiagnosticMedic diagnostic = DiagnosticMedic.builder()
                    .pacient(pacient)
                    .diagnosticFinal(diagnosticFinal)
                    .observatiiClinice(observatiiClinice)
                    .build();

            diagnosticMedicService.salveazaDiagnostic(diagnostic, chestionareIds, analizeIds);

            redirectAttributes.addFlashAttribute("success", "Diagnosticul a fost salvat cu succes.");

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.CREARE,
                    AuditLog.TipResursa.DIAGNOSTIC,
                    diagnostic.getId(),
                    pacient.getId()
            );

        } catch (Exception e) {
            log.error("Eroare la salvarea diagnosticului pentru pacientul {}: {}", pacientId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Eroare la salvarea diagnosticului: " + e.getMessage());
        }

        return "redirect:/medic/" + medicUserId + "/pacient/" + pacientId;
    }
}