package com.therapeutica.therapeutica_app.raspunsuri_intrebari.ui;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariService;
import com.therapeutica.therapeutica_app.raport_chestionar.ScoringService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.therapeutica.therapeutica_app.notificari.events.NotificareEvent;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import org.springframework.context.ApplicationEventPublisher;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

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
    private final PacientiRepository pacientiRepository;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/completare/{raspunsChestionarId}/salveaza")
    public String salveazaRaspunsuri(@PathVariable UUID raspunsChestionarId,
                                     @RequestParam Map<String, String> raspunsuri,
                                     RedirectAttributes redirectAttributes) {

        log.info("POST /raspunsuri/completare/{}/salveaza", raspunsChestionarId);

        try {
            // 1. Procesează răspunsurile
            raspunsuriIntrebariService.proceseazaRaspunsuri(raspunsChestionarId, raspunsuri);

            // 2. Calculează scorurile
            scoringService.calculeazaSiSalveazaScor(raspunsChestionarId);

            // 3. Obține USER ID-ul pacientului (id-ul din tabela utilizatori/sesiune)
            UUID userIdPacient = raspunsuriChestionareRepository.findUserIdByRaspunsChestionarId(raspunsChestionarId);

            if (userIdPacient == null) {
                throw new NotFoundException("User ID not found for raspuns chestionar: " + raspunsChestionarId);
            }

            // 4. Obține numele chestionarului
            String numeChestionar = raspunsuriChestionareRepository.findNumeChestionarById(raspunsChestionarId);

            // Logica notificare medic
            pacientiRepository.findByUserId(userIdPacient).ifPresent(pacient -> {
                if (pacient.getMedic() != null && pacient.getMedic().getUser() != null) {
                    UUID medicUserId = pacient.getMedic().getUser().getId();
                    String numePacient = (pacient.getUser() != null) ? pacient.getUser().getNume() : "Un pacient";

                    // Link-ul către vizualizarea răspunsurilor pentru medic

                    String linkPentruMedic = "/raspunsuri/vizualizare/" + raspunsChestionarId;

                    NotificareEvent event = new NotificareEvent(
                            medicUserId,
                            "Chestionar Completat: " + numeChestionar,
                            "Pacientul " + numePacient + " a finalizat completarea chestionarului.",
                            linkPentruMedic
                    );

                    eventPublisher.publishEvent(event);
                    log.info("Notificare trimisă medicului pentru chestionarul: {}", numeChestionar);
                }
            });


            redirectAttributes.addFlashAttribute("success",
                    "Chestionarul '" + numeChestionar + "' a fost completat cu succes!");

            return "redirect:/raspunsuri-chestionare/pacient/" + userIdPacient + "/disponibile";

        } catch (Exception e) {
            log.error("Eroare: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "❌ Eroare: " + e.getMessage());
            return "redirect:/chestionare/completare/" + raspunsChestionarId;
        }
    }

    /**
     * Endpoint pentru vizualizarea răspunsurilor (pentru medic)
     */


    @GetMapping("/vizualizare/{raspunsChestionarId}")
    @Transactional(readOnly = true)
    public String vizualizeazaRaspunsuri(@PathVariable UUID raspunsChestionarId,
                                         Model model,
                                         HttpSession session) {
        log.info("Vizualizare rezultate detaliate pentru ID: {}", raspunsChestionarId);

        // 1. Aducem entitatea principală
        var chestionar = raspunsuriChestionareRepository.findById(raspunsChestionarId)
                .orElseThrow(() -> new NotFoundException("Rezultatele chestionarului nu au fost găsite."));
        if (chestionar.getChestionar() != null) {
            chestionar.getChestionar().getNume();
        }
        if (chestionar.getPacient() != null && chestionar.getPacient().getUser() != null) {
            chestionar.getPacient().getUser().getNume(); // Trezește proxy-ul Pacient->User
        }

        if (chestionar.getRaspunsuri() != null) {
            chestionar.getRaspunsuri().forEach(r -> {
                if (r.getIntrebare() != null) r.getIntrebare().getTextIntrebare();
                if (r.getCategorie() != null) r.getCategorie().getNume();
            });
        }

        String medicIdStr = (String) session.getAttribute("userId");
        if (medicIdStr != null) {
            model.addAttribute("medicId", UUID.fromString(medicIdStr));
        } else {
            // Fallback: Încercăm să îl luăm din relația cu pacientul dacă există
            if (chestionar.getPacient() != null && chestionar.getPacient().getMedic() != null) {
                model.addAttribute("medicId", chestionar.getPacient().getMedic().getUser().getId());
            }
        }


        // Grupăm răspunsurile pe categorii
        Map<String, List<RaspunsuriIntrebari>> raspunsuriPeCategorii = chestionar.getRaspunsuri().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCategorie() != null ? r.getCategorie().getNume() : "Fără Categorie",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Generăm Map-ul cu detaliile categoriilor
        Map<String, CategoriiChestionare> categoriiMap = chestionar.getRaspunsuri().stream()
                .filter(r -> r.getCategorie() != null)
                .map(RaspunsuriIntrebari::getCategorie)
                .distinct()
                .collect(Collectors.toMap(CategoriiChestionare::getNume, c -> c, (c1, c2) -> c1));

        // Aducem sau Calculăm Rezultatul
        var rezultat = scoringService.calculeazaSiSalveazaScor(raspunsChestionarId);

        // Adăugăm TOATE datele în Model
        model.addAttribute("chestionar", chestionar);
        model.addAttribute("raspunsuriPeCategorii", raspunsuriPeCategorii);
        model.addAttribute("categoriiMap", categoriiMap);
        model.addAttribute("rezultat", rezultat);

        return "medic/vizualizare-rezultate";
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
