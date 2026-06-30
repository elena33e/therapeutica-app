package com.therapeutica.therapeutica_app.raspunsuri_intrebari;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import com.therapeutica.therapeutica_app.intrebari.IntrebariRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RaspunsuriIntrebariService {

    private final RaspunsuriIntrebariRepository raspunsuriIntrebariRepository;
    private final IntrebariRepository intrebariRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;

    /**
     * Procesează răspunsurile primite de la formular
     */
    public void proceseazaRaspunsuri(UUID raspunsChestionarId,
                                     Map<String, String> raspunsuriRaw) {

        log.info("Procesare răspunsuri pentru chestionar: {}", raspunsChestionarId);

        RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                .findById(raspunsChestionarId)
                .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

        int raspunsuriSalvate = 0;
        int raspunsuriErate = 0;

        for (Map.Entry<String, String> entry : raspunsuriRaw.entrySet()) {
            String key = entry.getKey();
            String valoare = entry.getValue();

            if (key.startsWith("intrebare_") && valoare != null && !valoare.trim().isEmpty()) {
                try {
                    String intrebareIdStr = key.substring(10);
                    UUID intrebareId = UUID.fromString(intrebareIdStr);

                    salveazaRaspuns(raspunsChestionar, intrebareId, valoare);
                    raspunsuriSalvate++;

                } catch (Exception e) {
                    log.error("Eroare la procesarea răspunsului {}: {}", key, e.getMessage());
                    raspunsuriErate++;
                }
            }
        }

        log.info("{} răspunsuri salvate, {} erori pentru chestionar {}",
                raspunsuriSalvate, raspunsuriErate, raspunsChestionarId);

        // Marchează chestionarul ca completat
        raspunsChestionar.setStatus(RaspunsuriChestionare.StatusRaspuns.COMPLETAT);
        raspunsChestionar.setCompletatLa(LocalDateTime.now());

        // Calculează scorul total
        Double scorTotal = calculeazaScorTotal(raspunsChestionarId);
        raspunsChestionar.setScorTotalGeneral(BigDecimal.valueOf(scorTotal));

        raspunsuriChestionareRepository.save(raspunsChestionar);
    }

    /**
     * Salvează un răspuns individual
     */
    private void salveazaRaspuns(RaspunsuriChestionare raspunsChestionar,
                                 UUID intrebareId, String valoare) {

        // Verifică dacă există deja răspuns
        Optional<RaspunsuriIntrebari> existent = raspunsuriIntrebariRepository
                .findByRaspunsChestionarIdAndIntrebareId(raspunsChestionar.getId(), intrebareId);

        RaspunsuriIntrebari raspuns;

        if (existent.isPresent()) {
            // Actualizează răspunsul existent
            raspuns = existent.get();
        } else {
            // Creează răspuns nou
            raspuns = new RaspunsuriIntrebari();
            raspuns.setRaspunsChestionar(raspunsChestionar);

            Intrebare intrebare = intrebariRepository.findById(intrebareId)
                    .orElseThrow(() -> new NotFoundException("Întrebarea nu a fost găsită"));
            raspuns.setIntrebare(intrebare);
            raspuns.setCategorie(intrebare.getCategorie());
        }

        // Procesează valoarea în funcție de tipul întrebării
        proceseazaValoareRaspuns(raspuns, valoare);

        raspunsuriIntrebariRepository.save(raspuns);
    }

    /**
     * Procesează valoarea răspunsului în funcție de tip
     */
    private void proceseazaValoareRaspuns(RaspunsuriIntrebari raspuns, String valoare) {
        Intrebare.TipIntrebare tip = raspuns.getIntrebare().getTipIntrebare();

        switch (tip) {
            case SCOR_0_3:
            case SCOR_1_5:
                raspuns.setScor(Integer.parseInt(valoare));
                break;

            case DA_NU:
                raspuns.setRaspunsText(valoare.equalsIgnoreCase("true") ? "DA" : "NU");
                break;

            case TEXT_LIBER:
            case TEXT_SCURT:
            case EMAIL:
            case TELEFON:
                raspuns.setRaspunsText(valoare.trim());
                break;

            case NUMERIC:
                try {
                    BigDecimal numericValue = new BigDecimal(valoare);
                    raspuns.setRaspunsNumeric(numericValue);
                } catch (NumberFormatException e) {
                    log.error("Valoare numerică invalidă: {}", valoare);
                    throw new IllegalArgumentException("Valoare numerică invalidă: " + valoare);
                }
                break;

            case DATA:
                try {
                    // Încearcă diferite formate de dată
                    LocalDate dateValue = LocalDate.parse(valoare,
                            DateTimeFormatter.ISO_LOCAL_DATE);
                    raspuns.setRaspunsData(dateValue.atStartOfDay());
                } catch (DateTimeParseException e) {
                    log.error("Format dată invalid: {}", valoare);
                    throw new IllegalArgumentException("Format dată invalid: " + valoare);
                }
                break;

            case MULTIPLE_CHOICE:
                // Salvează ca JSON
                raspuns.setRaspunsText(valoare);
                break;
        }
    }

    /**
     * Obține toate răspunsurile detaliate pentru un chestionar.
     */
    public List<RaspunsuriIntrebari> getRaspunsuriDetaliate(UUID raspunsChestionarId) {
        return raspunsuriIntrebariRepository.findByRaspunsChestionarIdWithDetails(raspunsChestionarId);
    }

    /**
     * Calculează scorul total pentru un chestionar
     */
    public Double calculeazaScorTotal(UUID raspunsChestionarId) {
        List<RaspunsuriIntrebari> raspunsuri = getRaspunsuriDetaliate(raspunsChestionarId);

        double suma = 0;
        int count = 0;

        for (RaspunsuriIntrebari raspuns : raspunsuri) {
            if (raspuns.getScor() != null) {
                suma += raspuns.getScor();
                count++;
            }
        }

        return count > 0 ? Math.round((suma / count) * 100.0) / 100.0 : 0.0;
    }

    /**
     * Analizează răspunsurile și generează interpretare
     */
    public Map<String, Object> analizeazaRaspunsuri(UUID raspunsChestionarId) {
        Map<String, Object> analiza = new HashMap<>();

        List<RaspunsuriIntrebari> raspunsuri = getRaspunsuriDetaliate(raspunsChestionarId);

        // Grupează pe categorii
        Map<String, List<RaspunsuriIntrebari>> raspunsuriPeCategorii = new HashMap<>();
        for (RaspunsuriIntrebari raspuns : raspunsuri) {
            if (raspuns.getCategorie() != null) {
                String categorieNume = raspuns.getCategorie().getNume();
                raspunsuriPeCategorii
                        .computeIfAbsent(categorieNume, k -> new ArrayList<>())
                        .add(raspuns);
            }
        }

        // Calculează scoruri per categorie
        Map<String, Double> scoruriCategorii = new HashMap<>();
        Map<String, String> interpretariCategorii = new HashMap<>();

        for (Map.Entry<String, List<RaspunsuriIntrebari>> entry :
                raspunsuriPeCategorii.entrySet()) {

            String categorieNume = entry.getKey();
            List<RaspunsuriIntrebari> raspunsuriCategorie = entry.getValue();

            double scorCategorie = calculeazaScorCategorie(raspunsuriCategorie);
            String interpretare = interpreteazaScor(categorieNume, scorCategorie);

            scoruriCategorii.put(categorieNume, scorCategorie);
            interpretariCategorii.put(categorieNume, interpretare);
        }

        analiza.put("scoruriCategorii", scoruriCategorii);
        analiza.put("interpretariCategorii", interpretariCategorii);
        analiza.put("totalRaspunsuri", raspunsuri.size());
        analiza.put("scorTotal", calculeazaScorTotal(raspunsChestionarId));

        return analiza;
    }

    private double calculeazaScorCategorie(List<RaspunsuriIntrebari> raspunsuri) {
        double suma = 0;
        int count = 0;

        for (RaspunsuriIntrebari raspuns : raspunsuri) {
            if (raspuns.getScor() != null) {
                suma += raspuns.getScor();
                count++;
            }
        }

        return count > 0 ? Math.round((suma / count) * 100.0) / 100.0 : 0.0;
    }

    private String interpreteazaScor(String categorie, double scor) {
        if (scor < 1.0) return "Normal";
        else if (scor < 1.5) return "Ușor afectat";
        else if (scor < 2.0) return "Moderat afectat";
        else if (scor < 2.5) return "Afectat";
        else return "Sever afectat";
    }

    public void stergeRaspuns(UUID raspunsIntrebareId) {
        raspunsuriIntrebariRepository.deleteById(raspunsIntrebareId);
        log.info("Răspuns șters: {}", raspunsIntrebareId);
    }



}