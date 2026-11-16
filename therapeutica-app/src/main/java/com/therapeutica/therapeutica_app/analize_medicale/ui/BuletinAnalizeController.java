package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.therapeutica.therapeutica_app.analize_medicale.BuletinAnalizeService;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.analize_medicale.dto.BuletinEditabilDTO;
import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.security.AccesPacientService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rutele de analize accesate de pacient, plus finalizarea mapării LOINC de către medic.
 *
 * Convenție de identificatori: rutele cu {pacientId} primesc userId (Utilizatori.id),
 * nu Pacienti.id. Verificările și auditul folosesc întotdeauna Pacienti.id.
 */
@Controller
@RequestMapping("/analize")
@RequiredArgsConstructor
@Slf4j
public class BuletinAnalizeController {

    private final BuletinAnalizeService analizeService;
    private final AuditService auditService;
    private final AccesPacientService accesPacientService;
    private final DocumentMedicalRepository documentMedicalRepository;

    /**
     * Afișează dosarul medical al pacientului.
     */
    @GetMapping("/pacient/documente/{pacientId}")
    public String afiseazaDosar(@PathVariable UUID pacientId, Model model, HttpSession session) {
        accesPacientService.verificaAccesPropriuByUserId(session, pacientId);

        log.info("Accesare dosar medical pentru utilizatorul: {}", pacientId);

        List<DocumentMedical> documente = analizeService.getDocumentePacient(pacientId);

        model.addAttribute("documente", documente);
        model.addAttribute("pacientId", pacientId);

        return "pacient/analize/dosar-medical";
    }

    /**
     * Pagina dedicată pentru încărcarea documentelor.
     */
    @GetMapping("/upload/{pacientId}")
    public String paginaUpload(@PathVariable UUID pacientId, Model model, HttpSession session) {
        accesPacientService.verificaAccesPropriuByUserId(session, pacientId);

        model.addAttribute("pacientId", pacientId);
        return "pacient/analize/upload-pdf";
    }

    /**
     * Servește fișierul fizic al unui document către pacientul care l-a încărcat.
     */
    @GetMapping("/view-pdf/{documentId}")
    public ResponseEntity<Resource> vizualizeazaFisierFizic(@PathVariable UUID documentId,
                                                            HttpSession session) {
        DocumentMedical doc = documentMedicalRepository.findByIdWithPacientSiUser(documentId)
                .orElseThrow(() -> new NotFoundException("Documentul nu a fost găsit."));

        accesPacientService.verificaAccesPropriu(session, doc.getPacient().getId());

        try {
            Path caleFolder = Paths.get(doc.getCaleFisierStocare());

            Optional<Path> fisierOptional;
            try (var continut = Files.list(caleFolder)) {
                fisierOptional = continut.filter(Files::isRegularFile).findFirst();
            }

            if (fisierOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path fisier = fisierOptional.get();
            Resource resource = new UrlResource(fisier.toUri());

            String contentType = Files.probeContentType(fisier);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.VIZUALIZARE,
                    AuditLog.TipResursa.DOCUMENT_MEDICAL,
                    documentId,
                    doc.getPacient().getId()
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Eroare la servirea fișierului pentru documentul {}: {}", documentId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Încarcă documentele și pornește procesarea OCR asincronă.
     */
    @PostMapping("/incarca")
    public String incarcaSiProceseaza(@RequestParam("files") MultipartFile[] files,
                                      @RequestParam("pacientId") UUID pacientId,
                                      RedirectAttributes redirectAttributes,
                                      HttpSession session) {

        accesPacientService.verificaAccesPropriuByUserId(session, pacientId);

        if (files == null || files.length == 0 || files[0].isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Nu ai selectat niciun fișier.");
            return "redirect:/analize/upload/" + pacientId;
        }

        try {
            log.info("Încărcare {} fișiere pentru utilizatorul: {}", files.length, pacientId);

            DocumentMedical doc = analizeService.initializeazaDocument(files, pacientId);
            analizeService.proceseazaDocumentAsincron(doc.getId());

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.CREARE,
                    AuditLog.TipResursa.DOCUMENT_MEDICAL,
                    doc.getId(),
                    doc.getPacient().getId()
            );

            redirectAttributes.addFlashAttribute("success",
                    "Cele " + files.length + " fișiere sunt în curs de procesare.");

        } catch (Exception e) {
            log.error("Eroare la încărcarea documentelor: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }

        return "redirect:/analize/pacient/documente/" + pacientId;
    }

    /**
     * Pagina de validare a datelor extrase prin OCR.
     */
    @GetMapping("/valideaza/{documentId}")
    public String paginaValidare(@PathVariable UUID documentId, Model model, HttpSession session) {
        DocumentMedical doc = documentMedicalRepository.findByIdWithPacientSiUser(documentId)
                .orElseThrow(() -> new NotFoundException("Documentul nu a fost găsit."));

        accesPacientService.verificaAccesPropriu(session, doc.getPacient().getId());

        BuletinEditabilDTO dto = analizeService.mapeazaDinDocumentValidat(doc);

        model.addAttribute("buletin", dto);
        model.addAttribute("pacientId", dto.getPacientId());

        return "pacient/analize/validare-tabel";
    }

    /**
     * Salvează datele validate de pacient și declanșează standardizarea semantică.
     */
    @PostMapping("/salveaza-validat")
    public String salveazaValidat(@ModelAttribute("buletin") BuletinEditabilDTO dto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model,
                                  HttpSession session) {

        DocumentMedical doc = documentMedicalRepository.findByIdWithPacientSiUser(dto.getDocumentId())
                .orElseThrow(() -> new NotFoundException("Documentul nu a fost găsit."));

        UUID pacientIdReal = doc.getPacient().getId();
        accesPacientService.verificaAccesPropriu(session, pacientIdReal);

        log.info("Salvare date validate pentru documentul: {}", dto.getDocumentId());

        if (bindingResult.hasErrors()) {
            log.error("Erori de binding pentru documentul {}: {}",
                    dto.getDocumentId(), bindingResult.getAllErrors());

            model.addAttribute("error", "Există erori în formatul datelor introduse.");
            return "pacient/analize/validare-tabel";
        }

        try {
            analizeService.salveazaDateValidate(dto);

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.MODIFICARE,
                    AuditLog.TipResursa.DOCUMENT_MEDICAL,
                    dto.getDocumentId(),
                    pacientIdReal
            );

            redirectAttributes.addFlashAttribute("success",
                    "Datele medicale au fost salvate și validate cu succes!");

            return "redirect:/analize/pacient/documente/" + dto.getPacientId();

        } catch (Exception e) {
            log.error("Eroare critică la salvarea documentului {}: {}",
                    dto.getDocumentId(), e.getMessage());

            model.addAttribute("buletin", dto);
            model.addAttribute("error", "Eroare la salvare: " + e.getMessage());
            return "pacient/analize/validare-tabel";
        }
    }

    /**
     * Vizualizarea în regim doar-citire a analizelor validate.
     */
    @GetMapping("/vizualizeaza/{documentId}")
    public String vizualizeazaAnalize(@PathVariable UUID documentId, Model model, HttpSession session) {
        DocumentMedical doc = documentMedicalRepository.findByIdWithPacientSiUser(documentId)
                .orElseThrow(() -> new NotFoundException("Documentul nu a fost găsit."));

        accesPacientService.verificaAccesPropriu(session, doc.getPacient().getId());

        BuletinEditabilDTO dto = analizeService.mapeazaDinDocumentValidat(doc);

        model.addAttribute("buletin", dto);
        model.addAttribute("pacientId", dto.getPacientId());
        model.addAttribute("citireaDoar", true);

        auditService.inregistreaza(
                auditService.utilizatorCurent(session),
                AuditLog.ActiuneAudit.VIZUALIZARE,
                AuditLog.TipResursa.DOCUMENT_MEDICAL,
                documentId,
                doc.getPacient().getId()
        );

        return "pacient/analize/vizualizare-analize";
    }

    /**
     * Ștergerea unui document medical de către pacient.
     */
    @PostMapping("/sterge/{documentId}")
    public String stergeDocument(@PathVariable UUID documentId,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {

        DocumentMedical doc = documentMedicalRepository.findByIdWithPacientSiUser(documentId)
                .orElseThrow(() -> new NotFoundException("Documentul nu a fost găsit."));

        UUID pacientIdReal = doc.getPacient().getId();
        accesPacientService.verificaAccesPropriu(session, pacientIdReal);

        // Ruta de dosar așteaptă userId, nu Pacienti.id
        UUID userId = doc.getPacient().getUser().getId();

        log.info("Cerere de ștergere pentru documentul: {}", documentId);

        try {
            analizeService.stergeDocument(documentId);

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.STERGERE,
                    AuditLog.TipResursa.DOCUMENT_MEDICAL,
                    documentId,
                    pacientIdReal
            );

            log.info("Documentul {} a fost șters.", documentId);
            redirectAttributes.addFlashAttribute("success", "Documentul a fost șters cu succes.");

        } catch (Exception e) {
            log.error("Eroare la ștergerea documentului {}: {}", documentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Eroare la ștergerea documentului: " + e.getMessage());
        }

        return "redirect:/analize/pacient/documente/" + userId;
    }

    /**
     * Salvarea corecțiilor LOINC făcute de medic (acțiune de medic, nu de pacient).
     */
    @PostMapping("/medic/finalizeaza/{documentId}")
    public String finalizeazaMapareMedic(@PathVariable UUID documentId,
                                         @ModelAttribute("buletin") BuletinEditabilDTO dto,
                                         RedirectAttributes redirectAttributes,
                                         HttpSession session) {

        DocumentMedical doc = documentMedicalRepository.findByIdWithPacientSiUser(documentId)
                .orElseThrow(() -> new NotFoundException("Documentul nu a fost găsit."));

        UUID pacientIdReal = doc.getPacient().getId();
        accesPacientService.verificaAccesMedic(session, pacientIdReal);

        log.info("Medic salvează maparea LOINC pentru documentul: {}", documentId);

        try {
            analizeService.salveazaCorectiiMedic(documentId, dto);

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.MODIFICARE,
                    AuditLog.TipResursa.DOCUMENT_MEDICAL,
                    documentId,
                    pacientIdReal
            );

            redirectAttributes.addFlashAttribute("success", "Maparea LOINC a fost validată cu succes!");

        } catch (Exception e) {
            log.error("Eroare la salvarea mapării de către medic: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare la validare: " + e.getMessage());
        }

        return "redirect:/medic/analize/dosar/" + pacientIdReal;
    }
}