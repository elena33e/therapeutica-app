package com.therapeutica.therapeutica_app.analize_medicale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.analize_medicale.dto.BuletinEditabilDTO;
import com.therapeutica.therapeutica_app.analize_medicale.dto.SectiuneWrapperDTO;
import com.therapeutica.therapeutica_app.notificari.events.NotificareEvent;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BuletinAnalizeService {
    private final PacientiRepository pacientiRepository;
    private final DocumentMedicalRepository documentMedicalRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate ocrRestTemplate;
    private final ApplicationEventPublisher eventPublisher;

    //Config URL-uri
    @Value("${external.services.python-server.base-url}")
    private String pythonBaseUrl;

    @Value("${external.services.python-ocr.path}")
    private String ocrPath;

    @Value("${external.services.python-semantic.path}")
    private String semanticPath;

    @Value("${external.services.python-interpret.path}")
    private String interpretPath;


    private final Path rootLocation = Paths.get("upload-dir/analize");

    /**
     * Încărcare fișier și Salvare Persistentă
     */

    @Transactional
    public DocumentMedical initializeazaDocument(MultipartFile file, UUID userIdDinSesiune) throws Exception {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fișierul este gol.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String sanitizedName = sanitizeFilename(originalFilename);

        // Logica de salvare fișier
        Files.createDirectories(rootLocation);
        String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedName;
        Path destinationFile = rootLocation.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        DocumentMedical doc = new DocumentMedical();

        doc.setPacientId(userIdDinSesiune);
        doc.setNumeFisier(originalFilename);
        doc.setCaleFisierStocare(destinationFile.toAbsolutePath().toString());
        doc.setStatus(DocumentMedical.StatusDocument.INCARCAT);

        DocumentMedical savedDoc = documentMedicalRepository.save(doc);
        log.info("Document salvat în DB. ID Document: {}", savedDoc.getId());

        return savedDoc;
    }
    /**
     * Șterge un document medical:
     * 1. Elimină fișierul fizic de pe disc
     * 2. (Opțional pe viitor) Curăță datele FHIR asociate
     * 3. Elimină înregistrarea din baza de date
     */
    /**
     * Ștergere hibridă (State-Dependent Deletion)
     */
    @Transactional
    public void stergeDocument(UUID documentId) {
        DocumentMedical doc = documentMedicalRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Eroare: Documentul nu există."));

        // Verificăm dacă documentul a "plecat" deja spre medic
        boolean esteOficializat = doc.getStatus() == DocumentMedical.StatusDocument.VALIDAT ||
                doc.getStatus() == DocumentMedical.StatusDocument.STANDARDIZAT ||
                doc.getStatus() == DocumentMedical.StatusDocument.INTERPRETAT;

        if (esteOficializat) {
            // SOFT DELETE: Păstrăm documentul pentru audit
            doc.setStatus(DocumentMedical.StatusDocument.STERS_DE_PACIENT);
            documentMedicalRepository.save(doc);
            log.info("Documentul {} a fost marcat ca STERS_DE_PACIENT (Soft Delete).", documentId);
        } else {
            // HARD DELETE: Ștergem complet (nu a fost văzut de nimeni)
            String caleFisier = doc.getCaleFisierStocare();
            if (StringUtils.hasText(caleFisier)) {
                try {
                    Files.deleteIfExists(Paths.get(caleFisier));
                    log.info("Fișierul fizic a fost șters: {}", caleFisier);
                } catch (Exception e) {
                    log.error("Eroare la ștergerea fișierului fizic {}: {}", caleFisier, e.getMessage());
                }
            }
            documentMedicalRepository.delete(doc);
            log.info("Documentul {} a fost șters definitiv din baza de date.", documentId);
        }
    }


    /**
     * Procesare OCR via Python
     */
    @Async
    public void proceseazaDocumentAsincron(DocumentMedical doc) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            File fileToSend = new File(doc.getCaleFisierStocare());
            if (!fileToSend.exists()) {
                log.error("Fișierul nu există la calea: {}", doc.getCaleFisierStocare());
                return;
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(fileToSend));
            body.add("pacientId", doc.getPacientId().toString());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String fullOcrUrl = pythonBaseUrl + ocrPath;
            log.info("--- [THREAD: {}] Începe OCR la {} pentru: {} ---",
                    Thread.currentThread().getName(), fullOcrUrl, doc.getNumeFisier());

            ResponseEntity<String> response = ocrRestTemplate.postForEntity(fullOcrUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                doc.setDateBrute(response.getBody());
                doc.setStatus(DocumentMedical.StatusDocument.PROCESAT);
                documentMedicalRepository.save(doc);
                log.info("--- OCR Finalizat cu succes ---");
            }
        } catch (Exception e) {
            log.error("Eroare în procesul OCR: {}", e.getMessage());
            doc.setStatus(DocumentMedical.StatusDocument.EROARE);
            documentMedicalRepository.save(doc);
        }
    }

    /**
     * Salvare finală după validarea UI și trimitere la Standardizare
     */
    @Transactional
    public void salveazaDateValidate(BuletinEditabilDTO dto) {
        DocumentMedical doc = documentMedicalRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Eroare: Documentul nu există."));

        try {
            // Găsim pacientul pe baza User ID-ului salvat în DTO
            Pacienti pacient = pacientiRepository.findByUserId(dto.getPacientId())
                    .orElseThrow(() -> new RuntimeException("Eroare: Pacientul nu a fost găsit."));

            // Salvăm datele validate de pacient
            String jsonValidat = objectMapper.writeValueAsString(dto.getSectiuni());
            doc.setDateValidate(jsonValidat);
            doc.setStatus(DocumentMedical.StatusDocument.VALIDAT);
            documentMedicalRepository.save(doc);

            // Logica notificare
            if (pacient.getMedic() != null && pacient.getMedic().getUser() != null) {
                UUID medicUserId = pacient.getMedic().getUser().getId();
                String numePacient = (pacient.getUser() != null) ? pacient.getUser().getNume() : "Un pacient";

                // Link-ul corect către dosarul medicului, folosind ID-ul intern al pacientului
                String linkPentruMedic = "/medic/analize/dosar/" + pacient.getId();

                NotificareEvent event = new NotificareEvent(
                        medicUserId,
                        "Buletin analize nou",
                        "Pacientul " + numePacient + " a încărcat un nou buletin de analize.",
                        linkPentruMedic
                );

                eventPublisher.publishEvent(event);
                log.info("Notificare trimisă către medicul {} pentru documentul validat.", medicUserId);
            } else {
                log.warn("Pacientul nu are medic asociat. Notificarea nu a fost trimisă.");
            }

            // 3. Lansăm procesarea asincronă către Python pentru standardizare
            this.trimiteSpreStandardizareSemantica(dto, pacient.getDataNasterii(), String.valueOf(pacient.getSex()));

        } catch (Exception e) {
            log.error("Eroare la salvarea datelor validate: {}", e.getMessage());
            throw new RuntimeException("A apărut o eroare la salvare.");
        }
    }

    @Async
    public void trimiteSpreStandardizareSemantica(BuletinEditabilDTO dto, LocalDate dataNasterii, String sex) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("buletin", dto);
            requestBody.put("sex", sex);
            requestBody.put("data_nasterii", dataNasterii.toString());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String fullSemanticUrl = pythonBaseUrl + semanticPath;
            log.info("--- Trimitere la Standardizare: {} ---", fullSemanticUrl);

            ResponseEntity<String> response = ocrRestTemplate.postForEntity(fullSemanticUrl, request, String.class);

            documentMedicalRepository.findById(dto.getDocumentId()).ifPresent(doc -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    doc.setDateStandardizate(response.getBody());
                    doc.setStatus(DocumentMedical.StatusDocument.STANDARDIZAT);
                } else {
                    doc.setStatus(DocumentMedical.StatusDocument.EROARE);
                }
                documentMedicalRepository.save(doc);
            });
        } catch (Exception e) {
            log.error("Eroare Standardizare Semantică: {}", e.getMessage());
        }
    }

    /**
     * PASUL A: Salvează corecțiile medicului
     */
    @Transactional
    public void salveazaCorectiiMedic(UUID docId, BuletinEditabilDTO dto) {
        DocumentMedical doc = documentMedicalRepository.findById(docId).orElseThrow();
        try {
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("documentId", docId.toString());
            wrapper.put("pacientId", dto.getPacientId().toString());
            wrapper.put("sectiuni", dto.getSectiuni());
            wrapper.put("status", "medic_validated");

            doc.setDateStandardizate(objectMapper.writeValueAsString(wrapper));
            doc.setStatus(DocumentMedical.StatusDocument.STANDARDIZAT);
            documentMedicalRepository.save(doc);
        } catch (Exception e) {
            throw new RuntimeException("Eroare salvare medic: " + e.getMessage());
        }
    }

    /**
     * PASUL B: Interpretare HPO (Finalizarea)
     */
    @Async
    public void finalizeazaInterpretareClinica(UUID documentId) {
        DocumentMedical doc = documentMedicalRepository.findById(documentId).orElseThrow();
        try {
            String payloadHpo = pregatestePayloadHpo(doc.getDateStandardizate(), doc.getId(), doc.getPacientId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payloadHpo, headers);

            // Folosim base-url + calea de interpretare
            String hpoUrl = pythonBaseUrl + interpretPath;

            log.info("--- Finalizare HPO la URL: {} ---", hpoUrl);
            ResponseEntity<String> response = ocrRestTemplate.postForEntity(hpoUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                doc.setDateInterpretate(response.getBody());
                doc.setStatus(DocumentMedical.StatusDocument.INTERPRETAT);
                documentMedicalRepository.save(doc);
            }
        } catch (Exception e) {
            log.error("Eroare la interpretarea HPO: {}", e.getMessage());
        }
    }

    // --- METODE UTILITARE ---

    private String pregatestePayloadHpo(String jsonStandardizat, UUID documentId, UUID pacientId) throws Exception {
        Map<String, Object> sursaMap = objectMapper.readValue(jsonStandardizat, new TypeReference<>() {});
        Map<String, Object> sectiuniMap = (Map<String, Object>) sursaMap.get("sectiuni");
        List<Map<String, Object>> indicatoriCurati = new ArrayList<>();

        for (Object sectiuneObj : sectiuniMap.values()) {
            Map<String, Object> sectiuneData = (Map<String, Object>) sectiuneObj;
            List<Map<String, Object>> indicatoriSursa = (List<Map<String, Object>>) sectiuneData.get("indicatori");
            if (indicatoriSursa != null) {
                for (Map<String, Object> ind : indicatoriSursa) {
                    String loincNum = (String) ind.get("loincNum");
                    if (loincNum != null && !loincNum.equals("Pending") && !loincNum.isEmpty()) {
                        Map<String, Object> indCurat = new HashMap<>();
                        indCurat.put("loinc_num", loincNum);
                        indCurat.put("nume_original", ind.get("nume"));
                        indCurat.put("valoare", ind.get("valoare"));
                        indCurat.put("um", ind.get("um"));
                        indCurat.put("interval", ind.get("interval"));
                        indicatoriCurati.add(indCurat);
                    }
                }
            }
        }
        Map<String, Object> payloadHpo = new HashMap<>();
        payloadHpo.put("documentId", documentId.toString());
        payloadHpo.put("pacientId", pacientId.toString());
        payloadHpo.put("indicatori", indicatoriCurati);
        return objectMapper.writeValueAsString(payloadHpo);
    }

    public BuletinEditabilDTO mapeazaDinDocumentValidat(DocumentMedical doc) {
        try {
            String jsonSursa = (doc.getStatus() == DocumentMedical.StatusDocument.STANDARDIZAT || doc.getStatus() == DocumentMedical.StatusDocument.INTERPRETAT)
                    ? doc.getDateStandardizate() : (doc.getStatus() == DocumentMedical.StatusDocument.VALIDAT ? doc.getDateValidate() : doc.getDateBrute());

            if (jsonSursa == null || jsonSursa.isEmpty()) return BuletinEditabilDTO.builder().documentId(doc.getId()).pacientId(doc.getPacientId()).sectiuni(new LinkedHashMap<>()).build();

            LinkedHashMap<String, SectiuneWrapperDTO> mapSectiuni = (jsonSursa.contains("\"sectiuni\""))
                    ? objectMapper.readValue(jsonSursa, BuletinEditabilDTO.class).getSectiuni()
                    : objectMapper.readValue(jsonSursa, new TypeReference<LinkedHashMap<String, SectiuneWrapperDTO>>() {});

            return BuletinEditabilDTO.builder().documentId(doc.getId()).pacientId(doc.getPacientId()).sectiuni(mapSectiuni).build();
        } catch (Exception e) {
            return BuletinEditabilDTO.builder().documentId(doc.getId()).pacientId(doc.getPacientId()).sectiuni(new LinkedHashMap<>()).build();
        }
    }

    public List<DocumentMedical> getDocumentePacient(UUID pacientId) {
        return documentMedicalRepository.findByPacientIdAndStatusNotOrderByDataIncarcareDesc(
                pacientId, DocumentMedical.StatusDocument.STERS_DE_PACIENT);
    }

    // Metoda pentru Medic - vede TOT (inclusiv cele retrase de pacient)
    public List<DocumentMedical> getDocumentePacientPentruMedic(UUID pacientId) {
        return documentMedicalRepository.findByPacientIdOrderByDataIncarcareDesc(pacientId);
    }

    private String getExtension(String filename) { return filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : ""; }

    private String sanitizeFilename(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
    }
}