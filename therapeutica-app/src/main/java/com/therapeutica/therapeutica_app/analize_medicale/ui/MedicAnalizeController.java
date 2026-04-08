package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.therapeutica.therapeutica_app.analize_medicale.BuletinAnalizeService;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.analize_medicale.dto.BuletinEditabilDTO;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/medic/analize")
@RequiredArgsConstructor
@Slf4j
public class MedicAnalizeController {

    private final BuletinAnalizeService analizeService;
    private final DocumentMedicalRepository documentMedicalRepository;
    private final PacientiRepository pacientiRepository;

    /**
     * 1. Lista de lucru a medicului (Worklist)
     * Afișează toate documentele care au statusul STANDARDIZAT și așteaptă revizuirea.
     */
    @GetMapping("/worklist")
    public String listaRevizuire(Model model) {
        log.info("Accesare worklist medic.");
        List<DocumentMedical> deRevizuit = documentMedicalRepository.findByStatus(DocumentMedical.StatusDocument.STANDARDIZAT);
        model.addAttribute("documente", deRevizuit);
        return "medic/analize/worklist";
    }

    /**
     * 2. Pagina de SPLIT-SCREEN (Revizuirea clinică)
     * Încarcă interfața unde medicul vede PDF-ul (stânga) și tabelul editabil (dreapta).
     */
    @GetMapping("/revizuire/{docId}")
    public String paginaRevizuire(@PathVariable UUID docId, Model model) {
        DocumentMedical doc = documentMedicalRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Documentul cu ID-ul " + docId + " nu a fost găsit."));

        // --- LOGICA NOUĂ PENTRU IMAGINE ---
        boolean isImage = false;
        try {
            // Detectăm tipul de fișier de pe disc (ex: image/png, application/pdf)
            String contentType = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(doc.getCaleFisierStocare()));
            if (contentType != null && contentType.startsWith("image/")) {
                isImage = true;
            }
        } catch (java.io.IOException e) {
            // Dacă e o eroare de citire, lăsăm isImage = false (va încerca să încarce iframe-ul default)
        }
        // ---------------------------------

        BuletinEditabilDTO dto = analizeService.mapeazaDinDocumentValidat(doc);

        model.addAttribute("buletin", dto);
        model.addAttribute("document", doc);
        model.addAttribute("isImage", isImage);

        return "medic/analize-pacient/revizuire-clinica";
    }

    /**
     * 3. Streaming PDF pentru Iframe
     * Această metodă servește fișierul fizic direct în browser pentru vizualizarea în paralel.
     */
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

    /**
     * 4. Salvarea modificărilor medicului și declanșarea HPO (Pasul Final)
     * Medicul apasă "Finalizează", datele se salvează și se trimit asincron către Python.
     */
    @PostMapping("/finalizeaza/{docId}")
    public String finalizeazaRevizuire(@PathVariable UUID docId,
                                       @ModelAttribute("buletin") BuletinEditabilDTO dto,
                                       RedirectAttributes redirectAttributes) {
        try {
            log.info("Medicul finalizează revizuirea pentru documentul: {}", docId);

            // A. Salvăm corecțiile (LOINC-urile alese/modificate de medic)
            analizeService.salveazaCorectiiMedic(docId, dto);

            // B. Declanșăm procesarea finală HPO/FHIR în Python
            analizeService.finalizeazaInterpretareClinica(docId);

            redirectAttributes.addFlashAttribute("success", "Buletinul a fost validat. Interpretarea HPO și resursele FHIR sunt în curs de generare.");
            return "redirect:/medic/analize/dosar-pacient/" + dto.getPacientId();

        } catch (Exception e) {
            log.error("Eroare critică la finalizarea revizuirii: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Eroare la salvarea datelor: " + e.getMessage());
            return "redirect:/medic/analize/revizuire/" + docId;
        }
    }


    /**
     * 7. Vizualizarea Raportului Clinic Final (FHIR & HPO)
     */
    @GetMapping("/vizualizeaza/{docId}")
    public String afiseazaRaportFinal(@PathVariable UUID docId, Model model) {
        log.info("Medic accesează raportul final pentru documentul: {}", docId);

        DocumentMedical doc = documentMedicalRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Documentul nu a fost găsit."));

        Pacienti pacient = pacientiRepository.findByUserId(doc.getPacientId())
                .orElseThrow(() -> new RuntimeException("Nu a fost găsit profilul de pacient asociat acestui document."));

        UUID realPacientId = pacient.getId();
        model.addAttribute("realPacientId", realPacientId);

        String fhirJson = doc.getDateInterpretate();

        if (fhirJson == null || fhirJson.isEmpty()) {
            log.warn("Raportul nu poate fi afișat: date_interpretate_fhir este gol pentru {}", docId);
            return "redirect:/medic/analize/dosar/" + realPacientId;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            // Parsăm FHIR-ul pentru afișarea de bază
            List<Map<String, Object>> observations = mapper.readValue(
                    fhirJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            model.addAttribute("observations", observations);
            model.addAttribute("document", doc);

            boolean hasHpo = observations.stream()
                    .anyMatch(obs -> obs.containsKey("extension") &&
                            !((List<?>) obs.get("extension")).isEmpty());

            model.addAttribute("hasHpo", hasHpo);


            // Apel catre motorul de diagnoza din Python

            // --- ÎN METODA afiseazaRaportFinal ---

            if (hasHpo) {
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    // Cererea sub formă de MAP
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("pacientId", realPacientId.toString());
                    payload.put("lab_data", observations);
                    payload.put("symptoms_data", new ArrayList<>());

                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                    String pythonApiUrl = "http://localhost:8000/differential-diagnosis";

                    ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, request, Map.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> diagnosisData = response.getBody();
                        model.addAttribute("diagnostice", diagnosisData.get("diagnostice"));
                        model.addAttribute("simptomeAnalizate", diagnosisData.get("simptome_analizate"));
                    }
                } catch (Exception e) {
                    log.error("Eroare la apelarea motorului de diagnoză Python: {}", e.getMessage());
                    model.addAttribute("diagnosticError", "Serviciul de diagnostic este momentan indisponibil.");
                }
            }

        } catch (JsonProcessingException e) {
            log.error("Eroare la parsarea JSON-ului FHIR: {}", e.getMessage());
            model.addAttribute("error", "Eroare tehnică la citirea datelor interpretate.");
        }

        return "medic/analize-pacient/raport-hpo";
    }

    /**
     * 5. Endpoint de căutare LOINC (API pentru Frontend)
     * Folosit de dropdown-ul de căutare din interfață când medicul vrea să schimbe un cod.
     * Poți să-l muți într-un @RestController separat dacă preferi, dar merge și aici cu @ResponseBody.
     */
    @GetMapping("/api/search-loinc")
    @ResponseBody
    public ResponseEntity<?> cautaCodLoinc(@RequestParam String query) {
        try {
            // Aici ar trebui să apelezi un serviciu care interoghează baza LOINC (Neo4j sau SQL)
            // Exemplu: return ResponseEntity.ok(analizeService.searchLoincTerms(query));
            log.info("Căutare LOINC pentru termenul: {}", query);
            return ResponseEntity.ok().build(); // Placeholder
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Eroare la căutare");
        }
    }

    /**
     * 6. Dosarul medical al unui pacient specific
     * Aici medicul vede istoricul analizelor și statusurile clinice pentru a iniția revizuiri.
     */
    @GetMapping("/dosar/{pacientId}")
    public String dosarPacientPentruMedic(@PathVariable UUID pacientId, Model model) {
        log.info("Medic accesează dosarul pacientului cu ID: {}", pacientId);

        // 1. Găsim entitatea pacient pentru a afla ce User ID are în spate
        Pacienti pacient = pacientiRepository.findById(pacientId)
                .orElseThrow(() -> new RuntimeException("Eroare: Pacientul nu a fost găsit în baza de date."));

        // 2. Extragem ID-ul utilizatorului din cont (deoarece documentele sunt salvate cu acest ID)
        UUID userId = pacient.getUser().getId();

        // 3. Căutăm documentele folosind userId-ul
        List<DocumentMedical> documente = documentMedicalRepository.findByPacientIdOrderByDataIncarcareDesc(userId);

        model.addAttribute("documente", documente);

        // Păstrăm pacientId în model pentru că interfața (butoane de back, URL-uri) are nevoie de el, nu de userId
        model.addAttribute("pacientId", pacientId);

        return "medic/analize-pacient/dosar-pacient";
    }
}
