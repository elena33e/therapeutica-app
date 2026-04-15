package com.therapeutica.therapeutica_app.diagnoza.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.diagnoza.DiagnozaService;
import com.therapeutica.therapeutica_app.diagnoza.IstoricDiagnoza;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/medic/pacienti")
@RequiredArgsConstructor
@Slf4j
public class DiagnozaController {

    private final PacientiRepository pacientiRepository;
    private final DiagnozaService diagnozaService;
    private final ObjectMapper objectMapper;

    /**
     * 1. RUTA DE AFIȘARE (GET)
     * Randează dovezile și citește ultimul JSON salvat în DB.
     */
    @GetMapping("/{pacientId}/diagnoza-integrata")
    public String afiseazaProfilIntegrat(@PathVariable UUID pacientId, Model model) {
        log.info("Accesare Hub Diagnoză pentru pacientul ID: {}", pacientId);

        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                .orElseThrow(() -> new RuntimeException("Pacientul nu a fost găsit."));

        UUID userId = pacient.getUser().getId();
        model.addAttribute("pacient", pacient);
        model.addAttribute("pacientUser", pacient.getUser());

        try {
            // 1. Extragere date de laborator (Am schimbat din Object în List pentru claritate)
            List<Object> analizeList = diagnozaService.extrageDateLab(userId);
            model.addAttribute("analizeInterpretate", analizeList);
            model.addAttribute("hasLabData", !analizeList.isEmpty());

            // 2. Extragere simptome (Corect tipizat acum)
            List<Map<String, Object>> simptome = diagnozaService.extrageSimptomeBrute(pacientId);
            model.addAttribute("simptomeExtrase", simptome);
            model.addAttribute("hasSimptome", !simptome.isEmpty());

            // 3. Verificăm istoricul
            Optional<IstoricDiagnoza> ultimaDiagnoza = diagnozaService.getUltimaDiagnoza(pacientId);

            if (ultimaDiagnoza.isPresent()) {
                Map<String, Object> diagnosisData = objectMapper.readValue(
                        ultimaDiagnoza.get().getRezultatJson(),
                        new TypeReference<>() {}
                );

                // Transmitem datele din JSON-ul salvat către UI
                model.addAttribute("diagnostice", diagnosisData.get("diagnostice"));
                model.addAttribute("simptomeAnalizate", diagnosisData.get("simptome_analizate"));
                model.addAttribute("dataRulare", ultimaDiagnoza.get().getDataRulare());
                model.addAttribute("diagnosticRulat", true);
            } else {
                model.addAttribute("diagnosticRulat", false);
            }

        } catch (Exception e) {
            log.error("Eroare la procesarea datelor clinice: {}", e.getMessage());
            model.addAttribute("error", "Nu s-au putut încărca dovezile clinice. Verificați integritatea datelor JSON.");
        }

        return "medic/profil-simptome-pacient";
    }

    /**
     * 2. RUTA DE PROCESARE (POST)
     * Apelată de buton, declanșează motorul, salvează și face redirect înapoi.
     */
    @PostMapping("/{pacientId}/ruleaza-diagnoza")
    public String declanseazaMotorDiagnoza(@PathVariable UUID pacientId, RedirectAttributes redirectAttributes) {
        log.info("Rulare cerută pentru motorul de diagnoză, pacient ID: {}", pacientId);

        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                .orElseThrow(() -> new RuntimeException("Pacientul nu a fost găsit."));

        try {
            // Apelează serviciul care interoghează Python și salvează în baza de date
            diagnozaService.ruleazaDiagnoza(pacient.getUser().getId(), pacientId);
            redirectAttributes.addFlashAttribute("success", "Evaluarea a fost realizată cu succes și salvată în istoric.");
        } catch (Exception e) {
            log.error("Eroare la generarea diagnozei: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("diagnosticError", "Serviciul de diagnostic integrat a întâmpinat o eroare: " + e.getMessage());
        }

        // Redirect-ul previne re-trimiterea formularului la F5
        return "redirect:/medic/pacienti/" + pacientId + "/diagnoza-integrata";
    }
}