package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalService;
import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.security.AccesPacientService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/medic/analize")
@RequiredArgsConstructor
public class MedicAnalizeController {

    private final DocumentMedicalService documentMedicalService;
    private final PacientiRepository pacientiRepository;
    private final AccesPacientService accesPacientService;
    private final AuditService auditService;

    @GetMapping("/dosar/{pacientId}")
    public String afiseazaDosar(
            @PathVariable UUID pacientId,
            HttpSession session,
            Model model) {

        accesPacientService.verificaAccesMedic(session, pacientId);

        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                .orElseThrow(() ->
                        new NotFoundException("Pacientul nu a fost găsit."));

        model.addAttribute("pacient", pacient);
        model.addAttribute("pacientUser", pacient.getUser());
        model.addAttribute("medicId", session.getAttribute("userId"));
        model.addAttribute(
                "documente",
                documentMedicalService.getDocumenteDupaPacientId(pacientId)
        );

        auditService.inregistreaza(
                auditService.utilizatorCurent(session),
                AuditLog.ActiuneAudit.VIZUALIZARE,
                AuditLog.TipResursa.DOSAR_PACIENT,
                pacientId,
                pacientId
        );

        return "medic/analize-pacient/dosar-pacient";
    }
}