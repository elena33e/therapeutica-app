package com.therapeutica.therapeutica_app.raspunsuri_intrebari.ui;

import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.notificari.events.NotificareEvent;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raport_chestionar.ScoringService;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;
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
    private final AuditService auditService;

    /**
     * Pacientul salvează răspunsurile completate la un chestionar.
     */
    @PostMapping("/completare/{raspunsChestionarId}/salveaza")
    public String salveazaRaspunsuri(@PathVariable UUID raspunsChestionarId,
                                     @RequestParam Map<String, String> raspunsuri,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {

        log.info("Salvare răspunsuri pentru chestionarul: {}", raspunsChestionarId);

        try {
            raspunsuriIntrebariService.proceseazaRaspunsuri(raspunsChestionarId, raspunsuri);
            scoringService.calculeazaSiSalveazaScor(raspunsChestionarId);

            UUID userIdPacient = raspunsuriChestionareRepository
                    .findUserIdByRaspunsChestionarId(raspunsChestionarId);

            if (userIdPacient == null) {
                throw new NotFoundException("User ID not found for raspuns chestionar: " + raspunsChestionarId);
            }

            String numeChestionar = raspunsuriChestionareRepository
                    .findNumeChestionarById(raspunsChestionarId);

            UUID pacientEntityId = null;
            Optional<Pacienti> pacientOptional = pacientiRepository.findByUserId(userIdPacient);

            if (pacientOptional.isPresent()) {
                Pacienti pacient = pacientOptional.get();
                pacientEntityId = pacient.getId();

                notificaMedicul(pacient, raspunsChestionarId, numeChestionar);
            }

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.CREARE,
                    AuditLog.TipResursa.CHESTIONAR,
                    raspunsChestionarId,
                    pacientEntityId
            );

            redirectAttributes.addFlashAttribute("success",
                    "Chestionarul '" + numeChestionar + "' a fost completat cu succes!");

            return "redirect:/chestionare/pacient/" + userIdPacient + "/disponibile";

        } catch (Exception e) {
            log.error("Eroare la salvarea răspunsurilor pentru chestionarul {}: {}",
                    raspunsChestionarId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Eroare: " + e.getMessage());
            return "redirect:/chestionare/completare/" + raspunsChestionarId;
        }
    }

    /**
     * Șterge un răspuns individual (pacientul își corectează o alegere).
     */
    @PostMapping("/sterge/{raspunsIntrebareId}")
    @ResponseBody
    public String stergeRaspuns(@PathVariable UUID raspunsIntrebareId) {
        try {
            raspunsuriIntrebariService.stergeRaspuns(raspunsIntrebareId);
            return "{\"status\": \"success\", \"message\": \"Răspuns șters cu succes\"}";
        } catch (Exception e) {
            log.error("Eroare la ștergerea răspunsului {}: {}", raspunsIntrebareId, e.getMessage());
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    // ========== METODE HELPER ==========

    /**
     * Trimite medicului o notificare că pacientul a finalizat chestionarul.
     */
    private void notificaMedicul(Pacienti pacient, UUID raspunsChestionarId, String numeChestionar) {
        if (pacient.getMedic() == null || pacient.getMedic().getUser() == null) {
            return;
        }

        UUID medicUserId = pacient.getMedic().getUser().getId();
        String numePacient = (pacient.getUser() != null) ? pacient.getUser().getNume() : "Un pacient";
        String linkPentruMedic = "/chestionare/medic/vizualizare/" + raspunsChestionarId;

        NotificareEvent event = new NotificareEvent(
                medicUserId,
                "Chestionar completat: " + numeChestionar,
                "Pacientul " + numePacient + " a finalizat completarea chestionarului.",
                linkPentruMedic
        );

        eventPublisher.publishEvent(event);
        log.info("Notificare trimisă medicului pentru chestionarul: {}", numeChestionar);
    }
}