package com.therapeutica.therapeutica_app.raspunsuri_intrebari;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import com.therapeutica.therapeutica_app.intrebari.IntrebariRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ValidareRaspunsuriService {

    private static final String PREFIX = "intrebare_";

    private final IntrebariRepository intrebariRepository;
    private final RaspunsuriChestionareRepository chestionareRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public record RezultatValidare(
            Map<String, String> raspunsuri,
            Map<String, String> erori
    ) {}

    private record ValoareEmail(@Email String valoare) {}

    @Transactional(readOnly = true)
    public RezultatValidare valideaza(
            UUID raspunsChestionarId,
            MultiValueMap<String, String> datePrimite) {

        var chestionar = chestionareRepository
                .findByIdForCompletare(raspunsChestionarId)
                .orElseThrow(() ->
                        new NotFoundException("Chestionarul nu a fost găsit.")
                );

        if (chestionar.getChestionar() == null
                || chestionar.getPacient() == null) {
            throw new NotFoundException(
                    "Datele asociate chestionarului sunt incomplete."
            );
        }

        String sexPacient = chestionar.getPacient().getSex() == null
                ? null
                : chestionar.getPacient().getSex().name();

        Map<String, Intrebare> intrebariPermise = new LinkedHashMap<>();

        for (Intrebare intrebare : intrebariRepository.findByChestionarId(
                chestionar.getChestionar().getId())) {

            if (esteCategorieAplicabila(intrebare.getCategorie(), sexPacient)) {
                intrebariPermise.put(PREFIX + intrebare.getId(), intrebare);
            }
        }

        Map<String, String> erori = new LinkedHashMap<>();
        Map<String, String> raspunsuriValidate = new LinkedHashMap<>();

        if (intrebariPermise.isEmpty()) {
            erori.put(
                    "_formular",
                    "Chestionarul nu conține întrebări aplicabile."
            );
        }

        // Refuză întrebările străine, neaplicabile sau cu identificatori invalizi.
        for (String cheie : datePrimite.keySet()) {
            if (cheie.startsWith(PREFIX)
                    && !intrebariPermise.containsKey(cheie)) {

                erori.put(
                        "_formular",
                        "Formularul conține întrebări nepermise. Reîncarcă pagina."
                );
            }
        }

        for (var entry : intrebariPermise.entrySet()) {
            String cheie = entry.getKey();
            Intrebare intrebare = entry.getValue();

            List<String> valoriBrute =
                    datePrimite.getOrDefault(cheie, List.of());

            List<String> valori = valoriBrute.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .toList();

            String eticheta = intrebare.getTextIntrebare() + ": ";

            if (valori.isEmpty()) {
                if (Boolean.TRUE.equals(intrebare.getObligatorie())) {
                    erori.put(cheie, eticheta + "răspuns obligatoriu.");
                }
                continue;
            }

            if (intrebare.getTipIntrebare() == null) {
                erori.put(
                        cheie,
                        eticheta + "tipul întrebării nu este configurat."
                );
                continue;
            }

            if (intrebare.getTipIntrebare()
                    != Intrebare.TipIntrebare.MULTIPLE_CHOICE
                    && valoriBrute.size() != 1) {

                erori.put(cheie, eticheta + "este permis un singur răspuns.");
                continue;
            }

            try {
                raspunsuriValidate.put(
                        cheie,
                        normalizeaza(intrebare, valori)
                );
            } catch (DateTimeParseException e) {
                erori.put(cheie, eticheta + "introdu o dată calendaristică validă.");
            } catch (NumberFormatException e) {
                erori.put(cheie, eticheta + "introdu un număr valid.");
            } catch (IllegalArgumentException e) {
                erori.put(cheie, eticheta + e.getMessage());
            }
        }

        return new RezultatValidare(raspunsuriValidate, erori);
    }

    public boolean esteCategorieAplicabila(
            CategoriiChestionare categorie,
            String sexPacient) {

        if (categorie == null) {
            return false;
        }

        return categorie.getSexTinta() == null
                || "AMBELE".equals(categorie.getSexTinta().name())
                || sexPacient == null
                || categorie.getSexTinta().name().equalsIgnoreCase(sexPacient);
    }

    private String normalizeaza(Intrebare intrebare, List<String> valori) {
        String valoare = valori.get(0);

        switch (intrebare.getTipIntrebare()) {
            case SCOR_0_3 -> {
                verifica(
                        valoare.matches("[0-3]"),
                        "alege un scor între 0 și 3."
                );
            }

            case DA_NU -> {
                verifica(
                        valoare.equals("true") || valoare.equals("false"),
                        "alege DA sau NU."
                );
            }

            case TEXT_SCURT -> verifica(
                    valoare.length() <= 255,
                    "sunt permise maximum 255 de caractere."
            );

            case TEXT_LIBER -> verifica(
                    valoare.length() <= 10_000,
                    "sunt permise maximum 10.000 de caractere."
            );

            case EMAIL -> verifica(
                    valoare.length() <= 254
                            && validator.validate(new ValoareEmail(valoare)).isEmpty(),
                    "introdu o adresă de email validă."
            );

            case TELEFON -> {
                String cifre = valoare.replaceAll("[^0-9]", "");

                verifica(
                        valoare.length() <= 40
                                && valoare.matches("\\+?[0-9() .-]+")
                                && cifre.length() >= 7
                                && cifre.length() <= 15,
                        "introdu un telefon cu 7–15 cifre, fără extensie."
                );
            }

            case NUMERIC -> {
                verifica(
                        valoare.length() <= 32
                                && valoare.matches("[-+]?[0-9]+(\\.[0-9]{1,2})?"),
                        "introdu un număr cu maximum două zecimale."
                );

                BigDecimal numar = new BigDecimal(valoare);

                verifica(
                        numar.abs().compareTo(new BigDecimal("99999999.99")) <= 0,
                        "numărul depășește limita permisă."
                );

                return numar.toPlainString();
            }

            case DATA -> {
                verifica(
                        valoare.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"),
                        "introdu data în formatul an-lună-zi."
                );

                LocalDate data = LocalDate.parse(valoare);

                verifica(
                        data.getYear() >= 1,
                        "anul trebuie să fie cel puțin 1."
                );

                return data.toString();
            }

            case MULTIPLE_CHOICE -> {
                List<String> optiuniPermise = citesteOptiuni(intrebare);
                Set<String> selectate = new LinkedHashSet<>(valori);

                verifica(
                        selectate.size() == valori.size(),
                        "aceeași opțiune a fost trimisă de mai multe ori."
                );

                verifica(
                        optiuniPermise.containsAll(selectate),
                        "selectează numai opțiuni din lista afișată."
                );

                try {
                    return objectMapper.writeValueAsString(
                            new ArrayList<>(selectate)
                    );
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(
                            "Opțiunile selectate nu au putut fi prelucrate.", e
                    );
                }
            }
        }

        return valoare;
    }

    public List<String> citesteOptiuni(Intrebare intrebare) {
        String json = intrebare.getOptiuniJson();

        verifica(
                json != null && !json.isBlank(),
                "opțiunile întrebării nu sunt configurate."
        );

        try {
            JsonNode lista = objectMapper.readTree(json);

            verifica(
                    lista != null && lista.isArray() && !lista.isEmpty(),
                    "opțiunile trebuie configurate ca o listă JSON de texte."
            );

            List<String> optiuni = new ArrayList<>();

            for (JsonNode element : lista) {
                verifica(
                        element.isTextual() && !element.asText().isBlank(),
                        "lista de opțiuni conține o valoare invalidă."
                );

                String optiune = element.asText().trim();

                verifica(
                        !optiuni.contains(optiune),
                        "lista de opțiuni conține duplicate."
                );

                optiuni.add(optiune);
            }

            return optiuni;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "opțiunile întrebării nu au un format JSON valid."
            );
        }
    }

    private void verifica(boolean conditie, String mesaj) {
        if (!conditie) {
            throw new IllegalArgumentException(mesaj);
        }
    }
}