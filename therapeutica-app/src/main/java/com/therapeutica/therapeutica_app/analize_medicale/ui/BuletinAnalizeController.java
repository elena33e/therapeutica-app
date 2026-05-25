package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.therapeutica.therapeutica_app.analize_medicale.BuletinAnalizeService;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.analize_medicale.dto.BuletinEditabilDTO;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/analize")
@RequiredArgsConstructor
@Slf4j
public class BuletinAnalizeController {

    private final BuletinAnalizeService analizeService;
    private final DocumentMedicalRepository documentMedicalRepository;
    private final PacientiRepository pacientiRepository;


    /**
     * Afișează "Dosarul Medical" al pacientului
     */
    @GetMapping("/pacient/documente/{pacientId}")
    public String afiseazaDosar(@PathVariable UUID pacientId, Model model) {
        log.info("Accesare dosar medical pentru pacient: {}", pacientId);
        List<DocumentMedical> documente = analizeService.getDocumentePacient(pacientId);
        model.addAttribute("documente", documente);
        model.addAttribute("pacientId", pacientId);
        return "pacient/analize/dosar-medical";
    }

    /**
     * Pagina dedicată pentru upload PDF
     */
    @GetMapping("/upload/{pacientId}")
    public String paginaUpload(@PathVariable UUID pacientId, Model model) {
        model.addAttribute("pacientId", pacientId);
        return "pacient/analize/upload-pdf";
    }

    @GetMapping("/view-pdf/{documentId}")
    public ResponseEntity<Resource> vizualizeazaFisierFizic(@PathVariable UUID documentId) {
        try {
            DocumentMedical doc = documentMedicalRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document negăsit"));

            Path caleFolder = Paths.get(doc.getCaleFisierStocare());

            // Cautăm primul fișier din folderul documentului
            Optional<Path> fisierOptional = Files.list(caleFolder)
                    .filter(Files::isRegularFile)
                    .findFirst();

            if (fisierOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path fisier = fisierOptional.get();
            Resource resource = new UrlResource(fisier.toUri());

            // Determinăm automat dacă e PDF, JPG, PNG etc.
            String contentType = Files.probeContentType(fisier);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    // "inline" îi spune browserului să îl afișeze în pagină, nu să îl descarce
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Eroare la servirea fișierului pentru documentul {}: {}", documentId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST - Trimite fișierul către Pipeline-ul Python (Docker)
     */
    @PostMapping("/incarca")
    public String incarcaSiProceseaza(@RequestParam("files") MultipartFile[] files,
                                      @RequestParam("pacientId") UUID pacientId,
                                      RedirectAttributes redirectAttributes) {

        if (files == null || files.length == 0 || files[0].isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Nu ai selectat niciun fișier.");
            return "redirect:/analize/upload/" + pacientId;
        }

        try {
            log.info("Încărcare {} fișiere pentru pacientul: {}", files.length, pacientId);
            DocumentMedical doc = analizeService.initializeazaDocument(files, pacientId);

            analizeService.proceseazaDocumentAsincron(doc.getId());

            redirectAttributes.addFlashAttribute("success", "Cele " + files.length + " fișiere sunt în curs de procesare.");
            return "redirect:/analize/pacient/documente/" + pacientId;

        } catch (Exception e) {
            log.error("Eroare la încărcare: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare: " + e.getMessage());
            return "redirect:/analize/pacient/documente/" + pacientId;
        }
    }

    /**
     * POST - Salvarea datelor după ce au fost verificate de utilizator în UI
     */
    @PostMapping("/salveaza-validat")
    public String salveazaValidat(@ModelAttribute("buletin") BuletinEditabilDTO dto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {

        log.info("Primire cerere salvare pentru documentul: {} al pacientului: {}",
                dto.getDocumentId(), dto.getPacientId());

        if (bindingResult.hasErrors()) {
            log.error("Eroare la binding date pentru documentul {}: {}",
                    dto.getDocumentId(), bindingResult.getAllErrors());

            // În caz de eroare de binding, trebuie să reafișăm pagina cu datele trimise de user
            model.addAttribute("error", "Există erori în formatul datelor introduse.");
            return "pacient/analize/validare-tabel";
        }

        try {
            analizeService.salveazaDateValidate(dto);
            redirectAttributes.addFlashAttribute("success", "✅ Datele medicale au fost salvate și validate cu succes!");

            // Redirecționăm către lista de documente a pacientului
            return "redirect:/analize/pacient/documente/" + dto.getPacientId();

        } catch (Exception e) {
            log.error("Eroare critică la salvarea documentului {}: {}", dto.getDocumentId(), e.getMessage());

            model.addAttribute("buletin", dto);
            model.addAttribute("error", "Eroare la salvare: " + e.getMessage());
            return "pacient/analize/validare-tabel";
        }
    }

    /**
     * POST - Salvarea corecțiilor LOINC făcute de MEDIC
     */
    /**
     * POST - Salvarea corecțiilor LOINC făcute de MEDIC
     */
    @PostMapping("/medic/finalizeaza/{documentId}")
    public String finalizeazaMapareMedic(@PathVariable UUID documentId,
                                         @ModelAttribute("buletin") BuletinEditabilDTO dto,
                                         RedirectAttributes redirectAttributes) {
        log.info("Medic salvează maparea LOINC pentru documentul: {}", documentId);

        // Căutăm entitatea Pacient
        log.info("PacientId primit din DTO: {}", dto.getPacientId());
        Optional<Pacienti> pacientOptional = pacientiRepository.findByUserId(dto.getPacientId());

        if (pacientOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Eroare: Pacientul nu a fost găsit în baza de date.");
            return "redirect:/medic/analize/dosar/" + dto.getPacientId();
        }

        // Extragem entitatea din Optional și obținem ID-ul ei real
        Pacienti pacient = pacientOptional.get();
        UUID pacientIdReal = pacient.getId();

        try {
            analizeService.salveazaCorectiiMedic(documentId, dto);
            redirectAttributes.addFlashAttribute("success", "Maparea LOINC a fost validată cu succes!");

            // 3. Redirecționăm folosind ID-ul corect (cheia primară a tabelului Pacienti)
            return "redirect:/medic/analize/dosar/" + pacientIdReal;

        } catch (Exception e) {
            log.error("Eroare la salvarea mapării de către medic: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare la validare: " + e.getMessage());

            return "redirect:/medic/analize/dosar/" + pacientIdReal;
        }
    }

    @GetMapping("/valideaza/{documentId}")
    public String paginaValidare(@PathVariable UUID documentId, Model model) {
        DocumentMedical doc = documentMedicalRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documentul nu a fost găsit."));

        BuletinEditabilDTO dto = analizeService.mapeazaDinDocumentValidat(doc);

        model.addAttribute("buletin", dto);
        model.addAttribute("pacientId", dto.getPacientId());

        return "pacient/analize/validare-tabel";
    }

    @GetMapping("/vizualizeaza/{documentId}")
    public String vizualizeazaAnalize(@PathVariable UUID documentId, Model model) {
        DocumentMedical doc = documentMedicalRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documentul nu a fost găsit."));

        BuletinEditabilDTO dto = analizeService.mapeazaDinDocumentValidat(doc);

        model.addAttribute("buletin", dto);
        model.addAttribute("pacientId", doc.getPacientId());
        model.addAttribute("citireaDoar", true);

        return "pacient/analize/vizualizare-analize";
    }

    /**
     * POST - Ștergerea unui document medical
     */
    @PostMapping("/sterge/{documentId}")
    public String stergeDocument(@PathVariable UUID documentId, RedirectAttributes redirectAttributes) {
        log.info("Primire cerere de ștergere pentru documentul: {}", documentId);

        // Extragem documentul mai întâi pentru a afla pacientId-ul necesar redirecționării
        DocumentMedical doc = documentMedicalRepository.findById(documentId).orElse(null);

        if (doc == null) {
            log.warn("Încercare de ștergere eșuată. Documentul {} nu există.", documentId);
            // Dacă nu avem pacientul, redirecționăm către o pagină generică (ajustează dacă ai o rută mai bună)
            redirectAttributes.addFlashAttribute("error", "Documentul nu a fost găsit.");
            return "redirect:/";
        }

        UUID pacientId = doc.getPacientId();

        try {
            // Apelăm serviciul pentru a executa ștergerea în cascadă (fișier, DB, FHIR)
            analizeService.stergeDocument(documentId);

            log.info("Documentul {} a fost șters cu succes.", documentId);
            redirectAttributes.addFlashAttribute("success", "Documentul a fost șters cu succes.");
        } catch (Exception e) {
            log.error("Eroare la ștergerea documentului {}: {}", documentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare la ștergerea documentului: " + e.getMessage());
        }

        // Redirecționăm înapoi la dosarul pacientului
        return "redirect:/analize/pacient/documente/" + pacientId;
    }



}