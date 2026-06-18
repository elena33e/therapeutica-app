package com.therapeutica.therapeutica_app.analize_medicale;

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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuletinAnalizeService {

    private final PacientiRepository pacientiRepository;
    private final DocumentMedicalRepository documentMedicalRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate ocrRestTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${external.services.python-server.base-url}")
    private String pythonBaseUrl;

    @Value("${external.services.python-ocr.path}")
    private String ocrPath;

    @Value("${external.services.python-semantic.path}")
    private String semanticPath;

    @Value("${external.services.python-interpret.path}")
    private String interpretPath;


    private final Path rootLocation = Paths.get("upload-dir/analize");

    @Transactional
    public DocumentMedical initializeazaDocument(MultipartFile[] files, UUID userIdDinSesiune) throws Exception {


        Pacienti pacient = pacientiRepository.findByUserId(userIdDinSesiune)
                .orElseThrow(() -> new RuntimeException("Pacientul nu a fost găsit pentru acest utilizator!"));

        if (files == null || files.length == 0) throw new IllegalArgumentException("Nu au fost selectate fișiere.");


        // Generăm un ID unic pentru acest set de documente (buletin)
        UUID documentId = UUID.randomUUID();
        Path folderDocument = rootLocation.resolve(documentId.toString());
        Files.createDirectories(folderDocument);

        String numeAfisat = files[0].getOriginalFilename();
        if (files.length > 1) {
            numeAfisat += " (+ încă " + (files.length - 1) + " imagini)";
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String sanitizedName = sanitizeFilename(Objects.requireNonNull(file.getOriginalFilename()));
            Path destinationFile = folderDocument.resolve(sanitizedName);

            // Copiem fiecare fișier în folderul dedicat
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        DocumentMedical doc = new DocumentMedical();
        doc.setId(documentId);
        doc.setPacient(pacient);
        doc.setNumeFisier(numeAfisat);
        doc.setCaleFisierStocare(folderDocument.toAbsolutePath().toString());
        doc.setStatus(DocumentMedical.StatusDocument.INCARCAT);

        return documentMedicalRepository.save(doc);
    }

    @Async
    public void proceseazaDocumentAsincron(UUID documentId) {

        String caleStocare;
        UUID pacientId;


        try {
            // Acest findById își deschide singur o tranzacție scurtă, citește și o închide instant.
            DocumentMedical initialDoc = documentMedicalRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Documentul nu mai există în DB."));
            caleStocare = initialDoc.getCaleFisierStocare();
            pacientId = initialDoc.getPacient().getId();
        } catch (Exception e) {
            log.error("Eroare la citirea inițială a documentului {}: {}", documentId, e.getMessage());
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            Path path = Paths.get(caleStocare);
            if (Files.isDirectory(path)) {
                try (var stream = Files.list(path)) {
                    stream.filter(Files::isRegularFile).forEach(f -> {
                        body.add("file", new FileSystemResource(f.toFile()));
                    });
                }
            } else {
                body.add("file", new FileSystemResource(path.toFile()));
            }

            body.add("pacientId", pacientId.toString());
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Trimitere fișiere către Python OCR pentru documentul: {}", documentId);
            // Aici aplicația așteaptă după Python, DAR conexiunea la baza de date este LIBERĂ pentru restul sistemului!
            ResponseEntity<String> response = ocrRestTemplate.postForEntity(pythonBaseUrl + ocrPath, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Când Python a terminat, mai deschidem o micro-tranzacție doar pentru salvare
                documentMedicalRepository.findById(documentId).ifPresentOrElse(doc -> {
                    doc.setDateBrute(response.getBody());
                    doc.setStatus(DocumentMedical.StatusDocument.PROCESAT);

                    documentMedicalRepository.saveAndFlush(doc);
                    log.info("--- [SUCCESS] OCR finalizat și salvat pentru ID: {} ---", documentId);
                }, () -> log.error("Documentul a dispărut din DB în timpul procesării OCR!"));
            }

        } catch (Exception e) {
            log.error("Eroare critică în thread-ul async pentru documentul {}: {}", documentId, e.getMessage());

            documentMedicalRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(DocumentMedical.StatusDocument.EROARE);
                documentMedicalRepository.saveAndFlush(doc);
            });
        }
    }

    @Transactional
    public void stergeDocument(UUID documentId) {
        DocumentMedical doc = documentMedicalRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Eroare: Documentul nu există."));

        boolean esteOficializat = doc.getStatus() == DocumentMedical.StatusDocument.VALIDAT ||
                doc.getStatus() == DocumentMedical.StatusDocument.STANDARDIZAT ||
                doc.getStatus() == DocumentMedical.StatusDocument.INTERPRETAT;

        if (esteOficializat) {
            doc.setStatus(DocumentMedical.StatusDocument.STERS_DE_PACIENT);
            documentMedicalRepository.save(doc);
            log.info("Documentul {} a fost marcat ca STERS_DE_PACIENT (Soft Delete).", documentId);
        } else {
            String caleFisier = doc.getCaleFisierStocare();
            if (StringUtils.hasText(caleFisier)) {
                try {
                    Files.deleteIfExists(Paths.get(caleFisier));
                } catch (Exception e) {
                    log.error("Eroare la ștergerea fișierului fizic: {}", e.getMessage());
                }
            }
            documentMedicalRepository.delete(doc);
            log.info("Documentul {} a fost șters definitiv din DB.", documentId);
        }
    }

    @Transactional
    public void salveazaDateValidate(BuletinEditabilDTO dto) {
        DocumentMedical doc = documentMedicalRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Eroare: Documentul nu există."));

        try {
            // IMPORTANT: dto.getPacientId() provine din mapeazaDinDocumentValidat() și conține
            // deja Pacienti.id (entitatea reală), NU userId. Nu trebuie convertit din nou cu
            // findByUserId — îl luăm direct din documentul deja încărcat din DB.
            Pacienti pacient = doc.getPacient();
            if (pacient == null) {
                throw new RuntimeException("Documentul nu are un pacient asociat!");
            }

            // CONVERSIE: Listă -> Map (pentru a păstra formatul JSON așteptat de restul aplicației/Python)
            Map<String, SectiuneWrapperDTO> mapSectiuni = dto.getSectiuni().stream()
                    .collect(Collectors.toMap(
                            SectiuneWrapperDTO::getNume,
                            s -> s,
                            (v1, v2) -> v1, // În caz de nume duplicate, păstrăm prima
                            LinkedHashMap::new
                    ));

            String jsonValidat = objectMapper.writeValueAsString(mapSectiuni);
            doc.setDateValidate(jsonValidat);
            doc.setStatus(DocumentMedical.StatusDocument.VALIDAT);
            documentMedicalRepository.save(doc);

            // Notificare Medic
            if (pacient.getMedic() != null && pacient.getMedic().getUser() != null) {
                UUID medicUserId = pacient.getMedic().getUser().getId();
                String numePacient = (pacient.getUser() != null) ? pacient.getUser().getNume() : "Un pacient";
                String linkPentruMedic = "/medic/analize/dosar/" + pacient.getId();

                NotificareEvent event = new NotificareEvent(medicUserId, "Buletin analize nou",
                        "Pacientul " + numePacient + " a încărcat un nou buletin de analize.", linkPentruMedic);
                eventPublisher.publishEvent(event);
            }

            LocalDate dataNasterii = (pacient.getDataNasterii() != null)
                    ? pacient.getDataNasterii()
                    : extrageDataNasteriiDinCNP(pacient.getCnp());

            String dataNasteriiFinala = (dataNasterii != null) ? dataNasterii.toString() : "1900-01-01";
            String sexPacient = (pacient.getSex() != null) ? pacient.getSex().toString() : "NECUNOSCUT";

            // Trimitem Map-ul re-construit către Python
            this.trimiteSpreStandardizareSemantica(mapSectiuni, dto.getDocumentId(), pacient.getId(), dataNasteriiFinala, sexPacient);

        } catch (Exception e) {
            log.error("Eroare la salvarea datelor validate: {}", e.getMessage());
            throw new RuntimeException("A apărut o eroare la salvare.");
        }
    }

    @Async
    public void trimiteSpreStandardizareSemantica(Map<String, SectiuneWrapperDTO> sectiuniMap, UUID documentId, UUID pacientId, String dataNasterii, String sex) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            // Re-ambalăm pentru Python într-un format compatibil
            Map<String, Object> buletinMap = new HashMap<>();
            buletinMap.put("documentId", documentId);
            buletinMap.put("pacientId", pacientId);
            buletinMap.put("sectiuni", sectiuniMap);

            requestBody.put("buletin", buletinMap);
            requestBody.put("sex", sex);
            requestBody.put("data_nasterii", dataNasterii);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String fullSemanticUrl = pythonBaseUrl + semanticPath;

            ResponseEntity<String> response = ocrRestTemplate.postForEntity(fullSemanticUrl, request, String.class);

            documentMedicalRepository.findById(documentId).ifPresent(doc -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    doc.setDateStandardizate(response.getBody());
                    doc.setStatus(DocumentMedical.StatusDocument.STANDARDIZAT);
                } else {
                    doc.setStatus(DocumentMedical.StatusDocument.EROARE);
                }
                documentMedicalRepository.save(doc);
            });
        } catch (Exception e) {
            log.error("Eroare în Standardizare: {}", e.getMessage());
            documentMedicalRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(DocumentMedical.StatusDocument.EROARE);
                documentMedicalRepository.save(doc);
            });
        }
    }

    @Transactional
    public void salveazaCorectiiMedic(UUID docId, BuletinEditabilDTO dto) {
        DocumentMedical doc = documentMedicalRepository.findById(docId).orElseThrow();
        try {
            Map<String, SectiuneWrapperDTO> mapSectiuni = dto.getSectiuni().stream()
                    .collect(Collectors.toMap(
                            SectiuneWrapperDTO::getNume,
                            s -> s,
                            (v1, v2) -> v1,
                            LinkedHashMap::new
                    ));

            // Salvăm direct map-ul, fără wrapper — format consistent cu restul aplicației
            String jsonFinal = objectMapper.writeValueAsString(mapSectiuni);
            doc.setDateStandardizate(jsonFinal);
            doc.setStatus(DocumentMedical.StatusDocument.STANDARDIZAT);
            documentMedicalRepository.save(doc);

            log.info("Date salvate cu succes pentru documentul: {}", docId);

            // Declanșăm interpretarea clinică async
            finalizeazaInterpretareClinica(docId);

        } catch (Exception e) {
            log.error("Eroare la salvarea medicului", e);
            throw new RuntimeException("Eroare salvare medic: " + e.getMessage());
        }
    }

    @Async
    public void finalizeazaInterpretareClinica(UUID documentId) {
        try {
            DocumentMedical doc = documentMedicalRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document negăsit: " + documentId));

            String payloadHpo = pregatestePayloadHpo(
                    doc.getDateStandardizate(),
                    doc.getId(),
                    doc.getPacient().getId()
            );

            log.info("Trimitere payload HPO către Python pentru documentul: {}", documentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payloadHpo, headers);

            ResponseEntity<String> response = ocrRestTemplate.postForEntity(
                    pythonBaseUrl + interpretPath,
                    request,
                    String.class
            );

            documentMedicalRepository.findById(documentId).ifPresent(d -> {
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    d.setDateInterpretate(response.getBody());
                    d.setStatus(DocumentMedical.StatusDocument.INTERPRETAT);
                    log.info("Interpretare clinică salvată pentru documentul: {}", documentId);
                } else {
                    d.setStatus(DocumentMedical.StatusDocument.EROARE);
                    log.error("Python a returnat {} pentru documentul {}", response.getStatusCode(), documentId);
                }
                documentMedicalRepository.saveAndFlush(d);
            });

        } catch (Exception e) {
            log.error("Eroare la interpretarea HPO pentru documentul {}: {}", documentId, e.getMessage());
            documentMedicalRepository.findById(documentId).ifPresent(d -> {
                d.setStatus(DocumentMedical.StatusDocument.EROARE);
                documentMedicalRepository.saveAndFlush(d);
            });
        }
    }

    @Transactional(readOnly = true)
    public BuletinEditabilDTO mapeazaDinDocumentValidat(DocumentMedical doc) {
        try {
            String jsonSursa = switch (doc.getStatus()) {
                case STANDARDIZAT, INTERPRETAT -> doc.getDateStandardizate();
                case VALIDAT -> doc.getDateValidate();
                default -> doc.getDateBrute();
            };

            if (jsonSursa == null || jsonSursa.isEmpty()) {
                return BuletinEditabilDTO.builder().documentId(doc.getId()).pacientId(doc.getPacient().getId()).build();
            }

            // Citim JSON-ul (care este Map în DB)
            Map<String, SectiuneWrapperDTO> mapSursa;
            if (jsonSursa.contains("\"sectiuni\"")) {
                // Dacă JSON-ul este un obiect complex (are câmpul "sectiuni")
                Map<String, Object> wrapper = objectMapper.readValue(jsonSursa, new TypeReference<>() {});
                mapSursa = objectMapper.convertValue(wrapper.get("sectiuni"), new TypeReference<LinkedHashMap<String, SectiuneWrapperDTO>>() {});
            } else {
                // Dacă JSON-ul este direct Map-ul de secțiuni
                mapSursa = objectMapper.readValue(jsonSursa, new TypeReference<LinkedHashMap<String, SectiuneWrapperDTO>>() {});
            }

            // CONVERSIE: Map -> List (pentru UI)
            List<SectiuneWrapperDTO> listaSectiuni = mapSursa.entrySet().stream()
                    .map(entry -> {
                        SectiuneWrapperDTO wrapper = entry.getValue();
                        wrapper.setNume(entry.getKey());
                        return wrapper;
                    })
                    .collect(Collectors.toList());

            return BuletinEditabilDTO.builder()
                    .documentId(doc.getId())
                    .pacientId(doc.getPacient().getId())
                    .sectiuni(listaSectiuni)
                    .build();

        } catch (Exception e) {
            log.error("Eroare mapping DTO: {}", e.getMessage());
            return BuletinEditabilDTO.builder().documentId(doc.getId()).pacientId(doc.getPacient().getId()).sectiuni(new ArrayList<>()).build();
        }
    }

    // METODE UTILITARE

    private LocalDate extrageDataNasteriiDinCNP(String cnp) {
        if (cnp == null || cnp.trim().length() < 13) return null;
        try {
            String cnpClean = cnp.trim();
            int s = Character.getNumericValue(cnpClean.charAt(0));
            int aa = Integer.parseInt(cnpClean.substring(1, 3));
            int mm = Integer.parseInt(cnpClean.substring(3, 5));
            int zz = Integer.parseInt(cnpClean.substring(5, 7));
            int anPrefix = switch (s) {
                case 1, 2 -> 1900;
                case 3, 4 -> 1800;
                case 5, 6 -> 2000;
                default -> 1900;
            };
            return LocalDate.of(anPrefix + aa, mm, zz);
        } catch (Exception e) { return null; }
    }

    private String pregatestePayloadHpo(String jsonStandardizat, UUID documentId, UUID pacientId) throws Exception {
        Map<String, Object> sursaMap = objectMapper.readValue(jsonStandardizat, new TypeReference<>() {});

        // Suportă atât formatul direct {sectiune: {...}} cât și wrapper-ul {"sectiuni": {...}}
        Map<String, Object> sectiuniMap;
        if (sursaMap.containsKey("sectiuni")) {
            sectiuniMap = (Map<String, Object>) sursaMap.get("sectiuni");
        } else {
            sectiuniMap = sursaMap;
        }

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

        Map<String, Object> payload = new HashMap<>();
        payload.put("documentId", documentId.toString());
        payload.put("pacientId", pacientId.toString());
        payload.put("indicatori", indicatoriCurati);
        return objectMapper.writeValueAsString(payload);
    }

    public List<DocumentMedical> getDocumentePacient(UUID userIdDinSesiune) {

        Pacienti pacient = pacientiRepository.findByUserId(userIdDinSesiune)
                .orElseThrow(() -> new RuntimeException("Pacientul nu a fost găsit pentru acest utilizator!"));

        return documentMedicalRepository.findByPacient_IdAndStatusNotOrderByDataIncarcareDesc(
                pacient.getId(),
                DocumentMedical.StatusDocument.STERS_DE_PACIENT
        );
    }

    /**
     * Convertește Pacienti.id (cheia primară a entității Pacienti) în userId-ul
     * utilizatorului asociat (Utilizatori.id). Necesar pentru construirea unor
     * redirect-uri către rute care, din convenție, primesc userId în loc de
     * Pacienti.id (ex. /analize/pacient/documente/{userId}).
     */
    public UUID getUserIdDinPacientId(UUID pacientEntityId) {
        Pacienti pacient = pacientiRepository.findById(pacientEntityId)
                .orElseThrow(() -> new RuntimeException("Pacientul nu a fost găsit (id entitate: " + pacientEntityId + ")"));

        if (pacient.getUser() == null) {
            throw new RuntimeException("Pacientul " + pacientEntityId + " nu are un utilizator asociat!");
        }

        return pacient.getUser().getId();
    }

    private String sanitizeFilename(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
    }
}