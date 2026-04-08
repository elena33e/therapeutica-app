package com.therapeutica.therapeutica_app.analize_medicale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.analize_medicale.dto.BuletinEditabilDTO;
import com.therapeutica.therapeutica_app.analize_medicale.dto.IndicatorDTO;
import com.therapeutica.therapeutica_app.analize_medicale.dto.SectiuneWrapperDTO;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
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

    // Preluăm URL-ul din application.yml
    @Value("${external.services.python-ocr.url}")
    private String pythonApiUrl;

    @Value("${external.services.python-semantic.url}")
    private String pythonSemanticUrl;

    private final Path rootLocation = Paths.get("upload-dir/analize");

    /**
     * PASUL 1: Încărcare fișier și Salvare Persistentă
     */
    public DocumentMedical initializeazaDocument(MultipartFile file, UUID pacientId) throws Exception {
        if (file.isEmpty()) throw new IllegalArgumentException("Fișierul este gol.");

        // 1. Validare și Sanitizare nume (Eliminăm diacriticele și spațiile problematice)
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String sanitizedName = sanitizeFilename(originalFilename);

        String extension = getExtension(sanitizedName);
        if (!List.of("pdf", "jpg", "png").contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Formatul " + extension + " nu este acceptat pentru OCR.");
        }

        // 2. Salvare fizică pe disc
        Files.createDirectories(rootLocation);
        String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedName;
        Path destinationFile = rootLocation.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        // 3. Salvare inițială în DB (Status INCARCAT)
        DocumentMedical doc = new DocumentMedical();
        doc.setPacientId(pacientId);
        doc.setNumeFisier(originalFilename); // Păstrăm numele original pentru afișare UI
        doc.setCaleFisierStocare(destinationFile.toAbsolutePath().toString());
        doc.setStatus(DocumentMedical.StatusDocument.INCARCAT);

        return documentMedicalRepository.save(doc);
    }

    /**
     * PASUL 2: Procesare OCR via Python (Trimitere Multipart)
     */
    @Async
    public void proceseazaDocumentAsincron(DocumentMedical doc) { // Schimbat în void și redenumit


        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            File fileToSend = new File(doc.getCaleFisierStocare());
            if (!fileToSend.exists()) {
                log.error("Fisierul nu exista la calea: {}", doc.getCaleFisierStocare());
                return;
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(fileToSend));
            body.add("pacientId", doc.getPacientId().toString());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("--- [THREAD: {}] Incepe OCR pentru: {} ---", Thread.currentThread().getName(), doc.getNumeFisier());

            ResponseEntity<String> response = ocrRestTemplate.postForEntity(pythonApiUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String jsonBrut = response.getBody();

                doc.setDateBrute(jsonBrut);
                doc.setStatus(DocumentMedical.StatusDocument.PROCESAT);
                documentMedicalRepository.save(doc);

                log.info("--- [THREAD: {}] OCR Finalizat cu succes pentru document: {} ---",
                        Thread.currentThread().getName(), doc.getNumeFisier());
            }
        } catch (Exception e) {
            log.error("Eroare in procesul asincron pentru documentul {}: {}", doc.getId(), e.getMessage());
            doc.setStatus(DocumentMedical.StatusDocument.EROARE);
            documentMedicalRepository.save(doc);
        }
    }

    /**
     * PASUL 3: Salvare finală după ce utilizatorul a validat datele în UI
     */
    @Transactional
    public void salveazaDateValidate(BuletinEditabilDTO dto) {
        log.info("Tentativă salvare date validate pentru documentul: {}", dto.getDocumentId());

        DocumentMedical doc = documentMedicalRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Eroare: Documentul nu există."));

        try {
            if (dto.getSectiuni() == null || dto.getSectiuni().isEmpty()) {
                throw new RuntimeException("Buletinul nu conține nicio secțiune.");
            }

            // 1. Extragerea contextului pacientului (Data Nașterii și Sex)
            // Folosim pacientId-ul din DTO pentru a lua datele din SQL
            Pacienti pacient = pacientiRepository.findByUserId(dto.getPacientId())
                    .orElseThrow(() -> new RuntimeException("Eroare: Pacientul nu a fost găsit."));

            // 2. Salvăm local ce a corectat omul în SQL
            String jsonValidat = objectMapper.writeValueAsString(dto.getSectiuni());
            doc.setDateValidate(jsonValidat);
            doc.setStatus(DocumentMedical.StatusDocument.VALIDAT);
            documentMedicalRepository.save(doc);

            log.info("Succes SQL: Documentul {} salvat. Inițiem pipeline semantic pentru pacient sex: {}, data nasterii: {}",
                    doc.getId(), pacient.getSex(), pacient.getDataNasterii());

            // 3. Declanșăm procesarea semantică trimitand și CONTEXTUL
            // Modificăm semnătura metodei de mai jos să accepte și aceste date
            this.trimiteSpreStandardizareSemantica(dto, pacient.getDataNasterii(), String.valueOf(pacient.getSex()));

        } catch (JsonProcessingException e) {
            log.error("Eroare serializare: {}", e.getMessage());
            throw new RuntimeException("Format JSON invalid.");
        } catch (Exception e) {
            log.error("Eroare la salvare: {}", e.getMessage());
            throw new RuntimeException("A apărut o eroare la salvarea datelor.");
        }
    }

    public List<DocumentMedical> getDocumentePacient(UUID pacientId) {
        // Adaugă o sortare dacă vrei ca ultimele analize să apară primele în tabel
        return documentMedicalRepository.findByPacientIdOrderByDataIncarcareDesc(pacientId);
    }

    // --- Utilitare ---

    private String getExtension(String filename) {
        return filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
    }

    /**
     * Elimină diacriticele și înlocuiește spațiile cu underscore
     * Previne erorile de tip "File Not Found" în Python din cauza encoding-ului.
     */
    private String sanitizeFilename(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String result = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return result.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
    }


    public BuletinEditabilDTO mapeazaDinDocumentValidat(DocumentMedical doc) {
        try {
            String jsonSursa;

            // 1. Logica de selecție a JSON-ului corectată
            if (doc.getStatus() == DocumentMedical.StatusDocument.STANDARDIZAT ||
                    doc.getStatus() == DocumentMedical.StatusDocument.INTERPRETAT) {
                // AICI sunt codurile LOINC puse de Python
                jsonSursa = doc.getDateStandardizate();
            } else if (doc.getStatus() == DocumentMedical.StatusDocument.VALIDAT) {
                // Aici sunt corecțiile pacientului (fără LOINC)
                jsonSursa = doc.getDateValidate();
            } else {
                // OCR-ul brut
                jsonSursa = doc.getDateBrute();
            }

            if (jsonSursa == null || jsonSursa.trim().isEmpty()) {
                return BuletinEditabilDTO.builder()
                        .documentId(doc.getId())
                        .pacientId(doc.getPacientId())
                        .sectiuni(new LinkedHashMap<>())
                        .build();
            }

            LinkedHashMap<String, SectiuneWrapperDTO> mapSectiuni;

            if (jsonSursa.contains("\"sectiuni\"") && jsonSursa.contains("\"documentId\"")) {
                BuletinEditabilDTO wrapper = objectMapper.readValue(jsonSursa, BuletinEditabilDTO.class);
                mapSectiuni = wrapper.getSectiuni();
            } else {
                mapSectiuni = objectMapper.readValue(
                        jsonSursa,
                        new TypeReference<LinkedHashMap<String, SectiuneWrapperDTO>>() {}
                );
            }

            return BuletinEditabilDTO.builder()
                    .documentId(doc.getId())
                    .pacientId(doc.getPacientId())
                    .sectiuni(mapSectiuni)
                    .build();

        } catch (Exception e) {
            log.error("Eroare la deserializare JSON pentru documentul {}: {}", doc.getId(), e.getMessage());
            return BuletinEditabilDTO.builder()
                    .documentId(doc.getId())
                    .pacientId(doc.getPacientId())
                    .sectiuni(new LinkedHashMap<>())
                    .build();
        }
    }

    @Async
    public void trimiteSpreStandardizareSemantica(BuletinEditabilDTO dto, LocalDate dataNasterii, String sex) {

        try {
            log.info("--- [THREAD: {}] Începe Standardizarea Semantică pentru: {} (Sex: {}, Data Nașterii: {}) ---",
                    Thread.currentThread().getName(), dto.getDocumentId(), sex, dataNasterii);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construim un payload care include datele validate și contextul pacientului
            // Acest JSON va fi "busola" clinică pentru Python (LOINC mapping)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("buletin", dto);           // Conține documentId, pacientId și secțiunile validate
            requestBody.put("sex", sex);               // Folosit pentru a evita erori de gen
            requestBody.put("data_nasterii", dataNasterii.toString()); // Folosit pentru filtrare (Adult vs Făt/Copil)

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Trimitem cererea către containerul de Python
            ResponseEntity<String> response = ocrRestTemplate.postForEntity(pythonSemanticUrl, request, String.class);

            documentMedicalRepository.findById(dto.getDocumentId()).ifPresentOrElse(doc -> {
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    doc.setDateStandardizate(response.getBody());
                    doc.setStatus(DocumentMedical.StatusDocument.STANDARDIZAT);
                    log.info("Succes: Rezultate LOINC ancorate clinic au fost salvate.");
                } else {
                    doc.setStatus(DocumentMedical.StatusDocument.EROARE);
                    log.error("Python a eșuat în procesarea semantică. Status cod: {}", response.getStatusCode());
                }
                documentMedicalRepository.save(doc);
            }, () -> log.error("Documentul {} nu a fost găsit în baza de date.", dto.getDocumentId()));

        } catch (Exception e) {
            log.error("Eroare critică în Standardizarea Semantică: {}", e.getMessage());
            documentMedicalRepository.findById(dto.getDocumentId()).ifPresent(doc -> {
                doc.setStatus(DocumentMedical.StatusDocument.EROARE);
                documentMedicalRepository.save(doc);
            });
        }
    }


    /**
     * PASUL A: Salvează ce a modificat medicul în interfață.
     * Aceasta trebuie apelată PRIMA în controller.
     */
    @Transactional
    public void salveazaCorectiiMedic(UUID docId, BuletinEditabilDTO dto) {
        DocumentMedical doc = documentMedicalRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document negăsit"));

        try {
            // Construim "Plicul" final care va fi salvat în coloana date_standardizate
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("documentId", docId.toString());
            wrapper.put("pacientId", dto.getPacientId().toString());
            wrapper.put("sectiuni", dto.getSectiuni()); // Aici sunt noile coduri LOINC din dropdown
            wrapper.put("status", "medic_validated");

            String jsonFinal = objectMapper.writeValueAsString(wrapper);

            // Salvăm în coloana pe care o citește pasul de HPO
            doc.setDateStandardizate(jsonFinal);
            doc.setStatus(DocumentMedical.StatusDocument.STANDARDIZAT);

            documentMedicalRepository.save(doc);
            log.info("✅ Datele validate de medic au fost persistate pentru documentul {}", docId);

        } catch (Exception e) {
            log.error("❌ Eroare la salvarea JSONB: {}", e.getMessage());
            throw new RuntimeException("Eroare salvare: " + e.getMessage());
        }
    }

    /**
     * PASUL B: Declanșează procesarea HPO în Python.
     * Aceasta este @Async pentru a nu bloca medicul în UI.
     */
    @Async
    public void finalizeazaInterpretareClinica(UUID documentId) {
        DocumentMedical doc = documentMedicalRepository.findById(documentId).orElseThrow();

        try {
            // 1. Pregătim payload-ul curat (folosind datele tocmai salvate de medic)
            String payloadHpo = pregatestePayloadHpo(doc.getDateStandardizate(), doc.getId(), doc.getPacientId());

            // 2. Apel Python
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payloadHpo, headers);

            // Înlocuim URL-ul de standardizare cu cel de interpretare
            String hpoUrl = pythonSemanticUrl.replace("standardize-results", "interpret-clinical-data");
            ResponseEntity<String> response = ocrRestTemplate.postForEntity(hpoUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                doc.setDateInterpretate(response.getBody());
                doc.setStatus(DocumentMedical.StatusDocument.INTERPRETAT);
                documentMedicalRepository.save(doc);
                log.info("✅ Interpretare HPO finalizată pentru doc: {}", documentId);
            }
        } catch (Exception e) {
            log.error("❌ Eroare la interpretarea HPO: {}", e.getMessage());
        }
    }

    /**
     * UTILITAR: Curăță JSON-ul "murdar" de scoruri pentru a fi acceptat de Pydantic în Python.
     */
    private String pregatestePayloadHpo(String jsonStandardizat, UUID documentId, UUID pacientId) throws Exception {
        // 1. Citim JSON-ul complex salvat de medic
        Map<String, Object> sursaMap = objectMapper.readValue(jsonStandardizat, new TypeReference<>() {});

        // 2. Extragem MAP-ul de secțiuni (nu mai avem o listă simplă "data")
        Map<String, Object> sectiuniMap = (Map<String, Object>) sursaMap.get("sectiuni");

        if (sectiuniMap == null) {
            log.error("❌ Structura JSON invalidă: lipsește cheia 'sectiuni'");
            throw new RuntimeException("Nu s-au găsit secțiuni în documentul standardizat.");
        }

        List<Map<String, Object>> indicatoriCurati = new ArrayList<>();

        // 3. Iterăm prin fiecare secțiune pentru a colecta toți indicatorii
        for (Object sectiuneObj : sectiuniMap.values()) {
            Map<String, Object> sectiuneData = (Map<String, Object>) sectiuneObj;
            List<Map<String, Object>> indicatoriSursa = (List<Map<String, Object>>) sectiuneData.get("indicatori");

            if (indicatoriSursa != null) {
                for (Map<String, Object> ind : indicatoriSursa) {
                    // Preluăm codul LOINC selectat de medic în dropdown
                    String loincNum = (String) ind.get("loincNum");

                    // Trimitem la HPO doar analizele care au un cod LOINC valid (nu Pending)
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

        // 4. Construim payload-ul "plat" cerut de Python (BuletinValidat)
        Map<String, Object> payloadHpo = new HashMap<>();
        payloadHpo.put("documentId", documentId.toString());
        payloadHpo.put("pacientId", pacientId.toString());
        payloadHpo.put("indicatori", indicatoriCurati);

        return objectMapper.writeValueAsString(payloadHpo);
    }
}