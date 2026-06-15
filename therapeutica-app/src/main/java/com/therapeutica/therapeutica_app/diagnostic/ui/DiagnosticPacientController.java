package com.therapeutica.therapeutica_app.diagnostic.ui;

import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedic;
import com.therapeutica.therapeutica_app.diagnostic.DiagnosticMedicService;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/pacient")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticPacientController {

    private final DiagnosticMedicService diagnosticMedicService;
    private final PacientiRepository pacientiRepository;

    @GetMapping("/istoric-diagnostice")
    public String afiseazaIstoricDiagnostice(HttpSession session, Model model) {
        String userIdStr = (String) session.getAttribute("userId");
        if (userIdStr == null) {
            log.warn("Tentativă de accesare istoric diagnostice fără sesiune validă.");
            return "redirect:/login";
        }

        try {
            UUID userId = UUID.fromString(userIdStr);

            // Găsim pacientul asociat userului logat
            Pacienti pacient = pacientiRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Pacientul nu a fost găsit pentru utilizatorul curent."));

            // Istoric diagnostice
            List<DiagnosticMedic> istoric = diagnosticMedicService.getIstoricDiagnosticPacientDetaliat(pacient.getId());

            // Sortare cronologică inversă
            if (istoric != null) {
                for (DiagnosticMedic diag : istoric) {
                    if (diag.getPacient() != null && diag.getPacient().getMedic() != null) {

                        diag.getPacient().getMedic().getUser().getNume();
                        diag.getPacient().getMedic().getUser().getPrenume();
                    }
                }
            }


            model.addAttribute("istoricDiagnostice", istoric);
            model.addAttribute("pacient", pacient);

            log.info("Istoric diagnostice încărcat pentru pacientul: {}", pacient.getId());
            return "pacient/istoric-diagnostice";

        } catch (Exception e) {
            log.error("Eroare la încărcarea istoricului de diagnostice: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare la încărcarea istoricului medical: " + e.getMessage());
            // Asigură-te că ai un template pentru erori la nivel de pacient, sau randează către un dashboard.
            return "pacient/error";
        }
    }
}