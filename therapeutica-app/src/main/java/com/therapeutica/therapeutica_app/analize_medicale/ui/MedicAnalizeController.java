package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.analize_medicale.BuletinAnalizeService;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.analize_medicale.dto.BuletinEditabilDTO;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@RequestMapping("/medic/analize")
@RequiredArgsConstructor
@Slf4j
public class MedicAnalizeController {

    private final BuletinAnalizeService analizeService;
    private final DocumentMedicalRepository documentMedicalRepository;
    private final PacientiRepository pacientiRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/revizuire/{docId}")
    public String paginaRevizuire(@PathVariable UUID docId, Model model) {
        DocumentMedical doc = documentMedicalRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Documentul cu ID-ul " + docId + " nu a fost găsit."));

        boolean isImage = false;
        try {
            String contentType = Files.probeContentType(Paths.get(doc.getCaleFisierStocare()));
            if (contentType != null && contentType.startsWith("image/")) {
                isImage = true;
            }
        } catch (IOException e) {
            log.warn("Nu s-a putut detecta tipul fișierului: {}", e.getMessage());
        }

        BuletinEditabilDTO dto = analizeService.mapeazaDinDocumentValidat(doc);

        // MODIFICARE: Accesăm direct entitatea pacient din document
        Pacienti pacient = doc.getPacient();
        if (pacient == null) {
            throw new RuntimeException("Documentul nu are un pacient asociat!");
        }

        model.addAttribute("buletin", dto);
        model.addAttribute("document", doc);
        model.addAttribute("isImage", isImage);
        model.addAttribute("pacientId", pacient.getId());

        return "medic/analize-pacient/revizuire-clinica";
    }

    @GetMapping("/view-pdf/{docId}")
    @ResponseBody
    public ResponseEntity<Resource> streamPdf(@PathVariable UUID docId) {
        DocumentMedical doc = documentMedicalRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Fișierul nu a fost găsit."));

        try {
            Path path = Paths.get(doc.getCaleFisierStocare());
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNumeFisier() + "\"")
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/finalizeaza/{docId}")
    public String finalizeazaRevizuire(@PathVariable UUID docId,
                                       @ModelAttribute("buletin") BuletinEditabilDTO dto,
                                       RedirectAttributes redirectAttributes) {
        try {
            log.info("Medicul finalizează revizuirea pentru documentul: {}", docId);

            analizeService.salveazaCorectiiMedic(docId, dto);
            analizeService.finalizeazaInterpretareClinica(docId);

            redirectAttributes.addFlashAttribute("success", "Buletinul a fost validat.");
            // Asigură-te că dto.getPacientId() este transmis corect din formular
            return "redirect:/medic/analize/dosar/" + dto.getPacientId();

        } catch (Exception e) {
            log.error("Eroare critică la finalizarea revizuirii: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare la salvarea datelor: " + e.getMessage());
            return "redirect:/medic/analize/revizuire/" + docId;
        }
    }

    @GetMapping("/vizualizeaza/{docId}")
    public String afiseazaRaportFinal(@PathVariable UUID docId, Model model) {
        log.info("Accesare raport vizualizare pentru documentul: {}", docId);

        DocumentMedical doc = documentMedicalRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Documentul nu a fost găsit."));

        // MODIFICARE: Accesăm direct entitatea pacient din document
        Pacienti pacient = doc.getPacient();

        model.addAttribute("realPacientId", pacient.getId());
        model.addAttribute("document", doc);

        try {
            List<Map<String, Object>> observations = objectMapper.readValue(
                    doc.getDateInterpretate(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            model.addAttribute("observations", observations);

            boolean hasHpoInLab = observations.stream()
                    .anyMatch(obs -> obs.containsKey("extension") && !((List<?>) obs.get("extension")).isEmpty());
            model.addAttribute("hasHpo", hasHpoInLab);

        } catch (Exception e) {
            log.error("Eroare la parsarea datelor FHIR: {}", e.getMessage());
            model.addAttribute("error", "Eroare la citirea datelor interpretate.");
        }

        return "medic/analize-pacient/raport-hpo";
    }

    @GetMapping("/dosar/{pacientId}")
    public String dosarPacientPentruMedic(@PathVariable UUID pacientId, Model model, HttpSession session) {
        log.info("Medic accesează dosarul pacientului cu ID: {}", pacientId);

        Object medicId = session.getAttribute("userId");

        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                .orElseThrow(() -> new RuntimeException("Eroare: Pacientul nu a fost găsit în baza de date."));

        Utilizatori pacientUser = pacient.getUser();
        List<DocumentMedical> documente = documentMedicalRepository.findByPacient_IdOrderByDataIncarcareDesc(pacientId);

        model.addAttribute("documente", documente);
        model.addAttribute("pacient", pacient);
        model.addAttribute("pacientUser", pacientUser);
        model.addAttribute("medicId", medicId);

        return "medic/analize-pacient/dosar-pacient";
    }
}