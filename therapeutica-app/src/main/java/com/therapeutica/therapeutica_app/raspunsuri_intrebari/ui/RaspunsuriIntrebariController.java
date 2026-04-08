package com.therapeutica.therapeutica_app.raspunsuri_intrebari.ui;

import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariService;
import com.therapeutica.therapeutica_app.raport_chestionar.ScoringService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/raspunsuri")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RaspunsuriIntrebariController {
    private final RaspunsuriIntrebariService raspunsuriIntrebariService;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final ScoringService scoringService;

    @PostMapping("/completare/{raspunsChestionarId}/salveaza")
    public String salveazaRaspunsuri(@PathVariable UUID raspunsChestionarId,
                                     @RequestParam Map<String, String> raspunsuri,
                                     RedirectAttributes redirectAttributes) {

        log.info("POST /raspunsuri/completare/{}/salveaza", raspunsChestionarId);

        try {
            // 1. Procesează răspunsurile (așa cum faci acum)
            raspunsuriIntrebariService.proceseazaRaspunsuri(raspunsChestionarId, raspunsuri);

            // 2. CALCULEAZĂ ȘI SALVEAZĂ SCORURILE
            scoringService.calculeazaSiSalveazaScor(raspunsChestionarId);

            // 3. Obține USER ID-ul pacientului
            UUID userId = raspunsuriChestionareRepository.findUserIdByRaspunsChestionarId(raspunsChestionarId);

            if (userId == null) {
                throw new NotFoundException("User ID not found for raspuns chestionar: " + raspunsChestionarId);
            }

            // 4. Obține numele chestionarului
            String numeChestionar = raspunsuriChestionareRepository
                    .findNumeChestionarById(raspunsChestionarId);

            // 5. Adaugă mesaj de succes
            redirectAttributes.addFlashAttribute("success",
                    "✅ Chestionarul '" + numeChestionar + "' a fost completat cu succes! Scorurile au fost calculate.");

            // 6. Redirecționează către pagina pacientului
            return "redirect:/raspunsuri-chestionare/pacient/" + userId + "/disponibile";

        } catch (Exception e) {
            log.error("❌ Eroare: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "❌ Eroare: " + e.getMessage());
            return "redirect:/chestionare/completare/" + raspunsChestionarId;
        }
    }

    /**
     * Endpoint pentru vizualizarea răspunsurilor (pentru medic)
     */
    @GetMapping("/vizualizare/{raspunsChestionarId}")
    public String vizualizeazaRaspunsuri(@PathVariable UUID raspunsChestionarId,
                                         org.springframework.ui.Model model) {

        // logica vizualizare raspunsuri de catre medic


        return "medic/vizualizare-raspunsuri";
    }

    /**
     * Endpoint pentru ștergerea unui răspuns (pentru pacient, dacă dorește să modifice)
     */
    @PostMapping("/sterge/{raspunsIntrebareId}")
    @ResponseBody
    public String stergeRaspuns(@PathVariable UUID raspunsIntrebareId) {
        try {
            raspunsuriIntrebariService.stergeRaspuns(raspunsIntrebareId);
            return "{\"status\": \"success\", \"message\": \"Răspuns șters cu succes\"}";
        } catch (Exception e) {
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }
}
