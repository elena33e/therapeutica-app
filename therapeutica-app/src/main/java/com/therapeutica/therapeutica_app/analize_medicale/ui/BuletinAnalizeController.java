package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.therapeutica.therapeutica_app.analize_medicale.BuletinAnalizeService;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.analize_medicale.dto.BuletinEditabilDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/analize")
@RequiredArgsConstructor
@Slf4j
public class BuletinAnalizeController {

    private final BuletinAnalizeService analizeService;
    private final DocumentMedicalRepository documentMedicalRepository;


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

    /**
     * POST - Trimite fișierul către Pipeline-ul Python (Docker)
     */
    @PostMapping("/incarca")
    public String incarcaSiProceseaza(@RequestParam("file") MultipartFile file,
                                      @RequestParam("pacientId") UUID pacientId,
                                      RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Fișierul este gol. Te rugăm să încarci un PDF valid.");
            return "redirect:/analize/upload/" + pacientId;
        }

        try {
            log.info("Inițializare document și lansare procesare asincronă pentru pacientul: {}", pacientId);

            // 1. Salvare fizică și creare record în DB (Status: INCARCAT)
            // Această metodă este sincronă și rapidă.
            DocumentMedical doc = analizeService.initializeazaDocument(file, pacientId);

            // 2. Lansare procesare OCR în fundal (Status se va schimba în PROCESAT când termină)
            // Se returnează controlul AICI imediat, fără să aștepte după Python.
            analizeService.proceseazaDocumentAsincron(doc);

            // 3. Notificăm utilizatorul și îl trimitem la dosar
            redirectAttributes.addFlashAttribute("success",
                    "Fișierul '" + file.getOriginalFilename() + "' a fost încărcat cu succes. " +
                            "Procesarea AI a început și poate dura 1-2 minute pentru documente mari.");

            return "redirect:/analize/pacient/documente/" + pacientId;

        } catch (Exception e) {
            log.error("Eroare la inițierea încărcării: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare la încărcare: " + e.getMessage());
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