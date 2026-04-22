package com.therapeutica.therapeutica_app.diagnoza;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnozaService {

    private final DocumentMedicalRepository documentMedicalRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final RaspunsuriIntrebariRepository raspunsuriIntrebariRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final IstoricDiagnozaRepository istoricDiagnozaRepository;
    private final PacientiRepository pacientiRepository;

    @Value("${external.services.python-server.base-url}")
    private String pythonBaseUrl;

    @Value("${external.services.python-diagnosis.path}")
    private String diagnosisPath;

    /**
     * Declanșează procesul de diagnoză integrată (Lab + Simptome)
     */
    public String ruleazaDiagnoza(UUID userId, UUID profilPacientId) {
        log.info("Inițiere diagnoză cumulativă. UserId: {}, ProfilId: {}", userId, profilPacientId);

        // Extragere date fuzionate din TOATE buletinele de analize
        List<Object> dateLab = extrageDateLab(userId);

        // Extragere simptome unice din chestionare, cu tot cu Clinical modifiers asociati
        List<Map<String, Object>> simptomeBrute = extrageSimptomeBrute(profilPacientId);

        log.info("Date colectate: {} analize unice, {} simptome unice.", dateLab.size(), simptomeBrute.size());

        if (dateLab.isEmpty() && simptomeBrute.isEmpty()) {
            throw new RuntimeException("Nu există date clinice (analize sau chestionare) pentru acest pacient.");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("pacientId", profilPacientId.toString());
        requestBody.put("lab_data", dateLab);
        requestBody.put("symptoms_data", simptomeBrute);

        // Apelare server Python
        String jsonRaspuns = trimiteSprePython(requestBody);

        // Salvare istoric în DB
        salveazaIstoric(profilPacientId, jsonRaspuns);

        return jsonRaspuns;
    }

    /**
     * Colectează analizele din documentele interpretate, păstrând doar cea mai recentă valoare pentru fiecare marker.
     */
    public List<Object> extrageDateLab(UUID userId) {
        List<DocumentMedical> documente = documentMedicalRepository.findByPacientIdOrderByDataIncarcareDesc(userId);

        List<Map<String, Object>> toateObservatiile = new ArrayList<>();

        for (DocumentMedical doc : documente) {
            if (doc.getStatus() == DocumentMedical.StatusDocument.INTERPRETAT && doc.getDateInterpretate() != null) {
                try {
                    List<Map<String, Object>> obsDinDoc = objectMapper.readValue(
                            doc.getDateInterpretate(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                    toateObservatiile.addAll(obsDinDoc);
                } catch (Exception e) {
                    log.error("Eroare la parsarea JSON pentru documentul {}: {}", doc.getId(), e.getMessage());
                }
            }
        }

        // Deduplicare după codul HPO: Dacă avem același marker în 2 buletine, 'toMap' îl păstrează pe primul (care e cel mai nou datorită sortării)
        return new ArrayList<>(
                toateObservatiile.stream()
                        .filter(obs -> extrageCodHPO(obs) != null)
                        .collect(Collectors.toMap(
                                this::extrageCodHPO,
                                obs -> obs,
                                (existent, nou) -> existent
                        ))
                        .values()
        );
    }


    /**
     * Extrage simptomele din chestionarele finalizate (COMPLETAT sau REVIZUIT),
     * deduplicând și păstrând relația cu clinical modifier-ul.
     */
    public List<Map<String, Object>> extrageSimptomeBrute(UUID profilPacientId) {
        // Folosim noua metodă IN pentru a aduce și chestionarele noi, și pe cele deja validate
        List<RaspunsuriChestionare> toateCompletarile = raspunsuriChestionareRepository
                .findByPacientIdAndStatusInFullRelations(
                        profilPacientId,
                        List.of(RaspunsuriChestionare.StatusRaspuns.COMPLETAT,
                                RaspunsuriChestionare.StatusRaspuns.REVIZUIT)
                );

        if (toateCompletarile.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> toateSimptomeleDetectate = new ArrayList<>();

        // Colectare simptome din toate chestionarele extrase
        for (RaspunsuriChestionare chestionar : toateCompletarile) {
            List<Map<String, Object>> simptomeDinChestionar = raspunsuriIntrebariRepository
                    .findByRaspunsChestionarIdWithDetails(chestionar.getId()).stream()
                    .filter(this::esteSimptomRelevant)
                    .map(ri -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("hpo_code", ri.getIntrebare().getHpoCode());
                        item.put("hpo_term", ri.getIntrebare().getHpoTerm());

                        if (ri.getIntrebare().getHpoTriggerCode() != null) {
                            item.put("trigger_code", ri.getIntrebare().getHpoTriggerCode());
                            item.put("trigger_term", ri.getIntrebare().getHpoTriggerTerm());
                        }
                        return item;
                    })
                    .toList();

            toateSimptomeleDetectate.addAll(simptomeDinChestionar);
        }

        return new ArrayList<>(
                toateSimptomeleDetectate.stream()
                        .collect(Collectors.toMap(
                                m -> (String) m.get("hpo_code"),
                                m -> m,
                                (existent, nou) -> existent
                        ))
                        .values()
        );
    }

    private String extrageCodHPO(Map<String, Object> obs) {
        try {
            List<Map<String, Object>> extensions = (List<Map<String, Object>>) obs.get("extension");
            if (extensions == null) return null;
            return extensions.stream()
                    .filter(ext -> "http://hl7.org/fhir/StructureDefinition/observation-phenotype".equals(ext.get("url")))
                    .map(ext -> (String) ((Map<String, Object>) ((List<Map<String, Object>>)
                            ((Map<String, Object>) ext.get("valueCodeableConcept")).get("coding")).get(0)).get("code"))
                    .findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private boolean esteSimptomRelevant(RaspunsuriIntrebari r) {
        Intrebare i = r.getIntrebare();
        if (i.getHpoCode() == null || r.getScor() == null) return false;
        return switch (i.getTipIntrebare().name()) {
            case "SCOR_0_3" -> r.getScor() >= 2;
            case "DA_NU" -> r.getScor() == 1;
            default -> false;
        };
    }

    private String trimiteSprePython(Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(pythonBaseUrl + diagnosisPath, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Eroare server Python: {}", e.getMessage());
            throw new RuntimeException("Motorul de diagnostic (Python) nu a putut fi contactat.");
        }
    }

    public Optional<IstoricDiagnoza> getUltimaDiagnoza(UUID pacientId) {
        return istoricDiagnozaRepository.findFirstByPacientIdOrderByDataRulareDesc(pacientId);
    }

    private void salveazaIstoric(UUID pacientId, String jsonRaspuns) {
        Pacienti pacient = pacientiRepository.findById(pacientId).orElseThrow();
        IstoricDiagnoza istoric = new IstoricDiagnoza();
        istoric.setPacient(pacient);
        istoric.setRezultatJson(jsonRaspuns);
        istoric.setDataRulare(LocalDateTime.now());
        istoricDiagnozaRepository.save(istoric);
    }
}