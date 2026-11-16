package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalService;
import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.security.AccesPacientService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/analize")
@RequiredArgsConstructor
@Slf4j
public class BuletinAnalizeController {

    private final DocumentMedicalService documentMedicalService;
    private final DocumentMedicalRepository documentMedicalRepository;
    private final AccesPacientService accesPacientService;
    private final AuditService auditService;

    // În această rută, pacientId reprezintă ID-ul utilizatorului.
    @GetMapping("/pacient/documente/{pacientId}")
    public String afiseazaDosar(
            @PathVariable UUID pacientId,
            Model model,
            HttpSession session) {

        accesPacientService.verificaAccesPropriuByUserId(session, pacientId);

        model.addAttribute(
                "documente",
                documentMedicalService.getDocumentePacient(pacientId)
        );
        model.addAttribute("pacientId", pacientId);

        return "pacient/analize/dosar-medical";
    }

    @GetMapping("/upload/{pacientId}")
    public String paginaUpload(
            @PathVariable UUID pacientId,
            HttpSession session) {

        accesPacientService.verificaAccesPropriuByUserId(session, pacientId);
        return "redirect:/analize/pacient/documente/" + pacientId;
    }

    @PostMapping("/incarca")
    public String incarcaDocumente(
            @RequestParam(value = "files", required = false)
            MultipartFile[] files,
            @RequestParam UUID pacientId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        accesPacientService.verificaAccesPropriuByUserId(session, pacientId);

        DocumentMedical document;

        try {
            document = documentMedicalService.incarcaDocumente(files, pacientId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/analize/pacient/documente/" + pacientId;
        } catch (Exception e) {
            log.error("Încărcarea documentelor a eșuat pentru {}", pacientId, e);
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Încărcarea a eșuat. Încearcă din nou."
            );
            return "redirect:/analize/pacient/documente/" + pacientId;
        }

        inregistreazaAudit(session, AuditLog.ActiuneAudit.CREARE, document);

        redirectAttributes.addFlashAttribute(
                "success",
                "Documentele au fost încărcate în dosarul medical."
        );

        return "redirect:/analize/pacient/documente/" + pacientId;
    }

    @PostMapping("/sterge/{documentId}")
    public String retrageDocument(
            @PathVariable UUID documentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        DocumentMedical document = documentMedicalRepository
                .findByIdWithPacientSiUser(documentId)
                .orElseThrow(() ->
                        new NotFoundException("Documentul nu a fost găsit."));

        accesPacientService.verificaAccesPropriu(
                session,
                document.getPacient().getId()
        );

        UUID userId = document.getPacient().getUser().getId();

        try {
            documentMedicalService.retrageDocument(documentId);
        } catch (Exception e) {
            log.error("Retragerea documentului {} a eșuat", documentId, e);
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Documentul nu a putut fi retras. Încearcă din nou."
            );
            return "redirect:/analize/pacient/documente/" + userId;
        }

        inregistreazaAudit(session, AuditLog.ActiuneAudit.STERGERE, document);

        redirectAttributes.addFlashAttribute(
                "success",
                "Documentul a fost retras din dosar."
        );

        return "redirect:/analize/pacient/documente/" + userId;
    }

    private void inregistreazaAudit(
            HttpSession session,
            AuditLog.ActiuneAudit actiune,
            DocumentMedical document) {

        try {
            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    actiune,
                    AuditLog.TipResursa.DOCUMENT_MEDICAL,
                    document.getId(),
                    document.getPacient().getId()
            );
        } catch (RuntimeException e) {
            log.error(
                    "Operația a fost salvată, dar auditul a eșuat pentru {}",
                    document.getId(),
                    e
            );
        }
    }
}