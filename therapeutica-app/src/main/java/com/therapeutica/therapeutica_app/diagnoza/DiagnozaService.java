package com.therapeutica.therapeutica_app.diagnoza;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnozaService {

    private final DocumentMedicalRepository documentMedicalRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final RaspunsuriIntrebariRepository raspunsuriIntrebariRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${external.services.python-semantic.url}")
    private String pythonBaseUrl;

    public String ruleazaDiagnoza(UUID pacientId) {
        log.info("Inițiere diagnoză on-demand pentru pacientul: {}", pacientId);

        // 1. Colectăm datele de laborator (cele deja interpretate/standardizate de Python anterior)
        Object dateLab = extrageDateLab(pacientId);

        // 2. Colectăm simptomele brute din chestionar (filtrate pentru zgomot)
        List<Map<String, String>> simptomeBrute = extrageSimptomeBrute(pacientId);

        // 3. Validare: Dacă nu avem nicio informație, nu are sens să apelăm Python
        if (dateLab == null && simptomeBrute.isEmpty()) {
            throw new RuntimeException("Nu există date (analize sau chestionar) pentru acest pacient.");
        }

        // 4. Construim Payload-ul către Python (Java nu face FHIR!)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("pacientId", pacientId.toString());
        requestBody.put("lab_data", dateLab); // JSON-ul LOINC deja existent
        requestBody.put("symptoms_data", simptomeBrute); // Listă de {hpo_code, hpo_term}

        // 5. Apelăm endpoint-ul de diagnostic din Python
        return trimiteSprePython(requestBody);
    }

    private Object extrageDateLab(UUID pacientId) {
        return documentMedicalRepository.findByPacientIdOrderByDataIncarcareDesc(pacientId).stream()
                .filter(doc -> doc.getStatus() == DocumentMedical.StatusDocument.INTERPRETAT)
                .findFirst()
                .map(doc -> {
                    try {
                        // Returnăm conținutul coloanei date_interpretate ca obiect/map
                        return objectMapper.readValue(doc.getDateInterpretate(), Object.class);
                    } catch (Exception e) { return null; }
                }).orElse(null);
    }

    private List<Map<String, String>> extrageSimptomeBrute(UUID pacientId) {
        // Luăm ultimul chestionar completat
        List<RaspunsuriChestionare> completari = raspunsuriChestionareRepository
                .findByPacientIdAndStatusFullRelations(pacientId, RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

        if (completari.isEmpty()) return new ArrayList<>();

        // Luăm răspunsurile individuale și aplicăm filtrul de zgomot clinic
        return raspunsuriIntrebariRepository
                .findByRaspunsChestionarIdWithDetails(completari.get(0).getId()).stream()
                .filter(this::esteSimptomRelevant)
                .map(r -> {
                    Map<String, String> simptom = new HashMap<>();
                    simptom.put("hpo_code", r.getIntrebare().getHpoCode());
                    simptom.put("hpo_term", r.getIntrebare().getHpoTerm());
                    return simptom;
                }).toList();
    }

    private boolean esteSimptomRelevant(RaspunsuriIntrebari r) {
        Intrebare i = r.getIntrebare();
        if (i.getHpoCode() == null || r.getScor() == null) return false;

        return switch (i.getTipIntrebare().name()) {
            case "SCOR_0_3" -> r.getScor() >= 2; // Pragul programatic pentru severitate
            case "DA_NU" -> r.getScor() == 1;   // Doar dacă răspunsul este DA
            default -> false;
        };
    }

    private String trimiteSprePython(Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = pythonBaseUrl.replace("standardize-results", "generate-integrated-diagnosis");
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Eroare la comunicarea cu motorul de diagnostic: {}", e.getMessage());
            throw new RuntimeException("Motorul de diagnostic nu a putut fi contactat.");
        }
    }
}