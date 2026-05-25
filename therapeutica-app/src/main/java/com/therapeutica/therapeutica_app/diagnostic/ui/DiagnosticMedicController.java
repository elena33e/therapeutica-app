package com.therapeutica.therapeutica_app.diagnostic.ui;

import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedic;
import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedicService;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticMedicController {

    private final DiagnosticMedicService diagnosticMedicService;
    private final PacientiRepository pacientiRepository;

    @PostMapping("/salveaza")
    public String salveazaDiagnostic(
            @RequestParam UUID pacientId,
            @RequestParam String diagnosticFinal,
            @RequestParam(required = false) String observatiiClinice,
            @RequestParam(required = false) List<UUID> chestionareIds,
            @RequestParam(required = false) List<UUID> analizeIds,
            RedirectAttributes redirectAttributes) {

        log.info("Medic salvează diagnostic pentru pacientul: {}", pacientId);

        try {
            Pacienti pacient = pacientiRepository.findById(pacientId)
                    .orElseThrow(() -> new IllegalArgumentException("Pacient invalid"));

            DiagnosticMedic diagnostic = DiagnosticMedic.builder()
                    .pacient(pacient)
                    .diagnosticFinal(diagnosticFinal)
                    .observatiiClinice(observatiiClinice)
                    .build();

            diagnosticMedicService.salveazaDiagnostic(diagnostic, chestionareIds, analizeIds);

            redirectAttributes.addFlashAttribute("success", "Diagnosticul a fost salvat cu succes.");

        } catch (Exception e) {
            log.error("Eroare la salvarea diagnosticului: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare la salvarea diagnosticului: " + e.getMessage());
        }

        return "redirect:/medic/pacienti/" + pacientId + "/diagnoza-integrata";
    }
}