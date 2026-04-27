package com.therapeutica.therapeutica_app.raport_chestionar;


import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariRepository;
import com.therapeutica.therapeutica_app.raport_chestionar.ScorCategorieDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScoringService {

    private final EntityManager entityManager;
    private final RaspunsuriIntrebariRepository raspunsuriIntrebariRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final RezultatScoringRepository rezultatScoringRepository;

    public RezultatScoring calculeazaSiSalveazaScor(UUID raspunsChestionarId) {
        log.info("Inițiere calcul scoring pentru ID: {}", raspunsChestionarId);

        // 1. Verificare existență și status recalculare
        Optional<RezultatScoring> existent = rezultatScoringRepository.findByRaspunsChestionarId(raspunsChestionarId);
        if (existent.isPresent() && !existent.get().getNecesitaRecalculare()) {
            log.info("Rezultat valid deja existent. Abandonare calcul.");
            return existent.get();
        }

        // 2. Încărcare entitate principală
        RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository.findById(raspunsChestionarId)
                .orElseThrow(() -> new RuntimeException("Eroare: Răspuns chestionar inexistent."));

        // 3. Extragere răspunsuri brute
        List<RaspunsuriIntrebari> toateRaspunsurile = raspunsuriIntrebariRepository.findByRaspunsChestionarId(raspunsChestionarId);
        log.info("Răspunsuri extrase din DB: {}", toateRaspunsurile.size());

        // 4. Grupare sigură după ID-ul categoriei (pentru a evita coliziuni de nume)
        Map<UUID, List<RaspunsuriIntrebari>> grupatePeId = toateRaspunsurile.stream()
                .filter(r -> r.getScor() != null && r.getCategorie() != null)
                .collect(Collectors.groupingBy(r -> r.getCategorie().getId()));

        log.info("Categorii identificate pentru procesare: {}", grupatePeId.size());

        // 5. Calcul scoruri per categorie
        List<ScorCategorieDTO> scoruriCategorii = grupatePeId.values().stream()
                .map(lista -> {
                    String nume = lista.get(0).getCategorie().getNume();
                    return calculeazaScorCategorie(nume, lista);
                })
                .collect(Collectors.toList());

        // 6. Calcul Scor Total General
        BigDecimal scorTotal = calculeazaScorTotal(scoruriCategorii);
        ScalaCromatica intervalTotal = ScalaCromatica.pentruScor(scorTotal.doubleValue());

        // 7. Colectare mape pentru entitate (cu protecție la duplicate)
        Map<String, BigDecimal> scoruriMap = scoruriCategorii.stream()
                .collect(Collectors.toMap(
                        ScorCategorieDTO::getNumeCategorie,
                        ScorCategorieDTO::getScorMediu,
                        (v1, v2) -> v1 // În caz de nume identice, păstrează prima valoare
                ));

        Map<String, String> culoriMap = scoruriCategorii.stream()
                .collect(Collectors.toMap(
                        ScorCategorieDTO::getNumeCategorie,
                        sc -> sc.getInterval().getCuloareHex(),
                        (v1, v2) -> v1
                ));

        Map<String, String> interpretariMap = scoruriCategorii.stream()
                .collect(Collectors.toMap(
                        ScorCategorieDTO::getNumeCategorie,
                        sc -> sc.getInterval().getInterpretare(),
                        (v1, v2) -> v1
                ));

        // 8. Statistici completare
        int totalIntrebari = getNumarTotalIntrebari(raspunsChestionar.getChestionar().getId());
        int raspunseCount = (int) toateRaspunsurile.stream()
                .filter(r -> r.getScor() != null || r.getRaspunsText() != null || r.getRaspunsNumeric() != null)
                .count();

        BigDecimal procentaj = totalIntrebari > 0
                ? BigDecimal.valueOf((double) raspunseCount / totalIntrebari * 100).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 9. Salvare/Actualizare entitate RezultatScoring
        RezultatScoring rezultat = existent.orElse(RezultatScoring.builder()
                .raspunsChestionar(raspunsChestionar)
                .build());

        rezultat.setScorTotal(scorTotal);
        rezultat.setCuloareTotalHex(intervalTotal.getCuloareHex());
        rezultat.setCuloareTotalNume(intervalTotal.getCuloareNume());
        rezultat.setInterpretareTotal(intervalTotal.getInterpretare());
        rezultat.setScoruriCategorii(scoruriMap);
        rezultat.setCuloriCategorii(culoriMap);
        rezultat.setInterpretariCategorii(interpretariMap);
        rezultat.setNumarTotalIntrebari(totalIntrebari);
        rezultat.setNumarIntrebariRaspunse(raspunseCount);
        rezultat.setProcentajCompletare(procentaj);
        rezultat.setCalculatLa(LocalDateTime.now());
        rezultat.setNecesitaRecalculare(false);
        rezultat.setVersiuneFormula("v1.1");

        RezultatScoring salvat = rezultatScoringRepository.save(rezultat);

        // 10. Sincronizare scor în tabela principală
        raspunsChestionar.setScorTotalGeneral(scorTotal);
        raspunsuriChestionareRepository.save(raspunsChestionar);

        log.info("✅ Scoring finalizat: {} categorii salvate. Scor total: {}", scoruriMap.size(), scorTotal);

        return salvat;
    }

    private ScorCategorieDTO calculeazaScorCategorie(String nume, List<RaspunsuriIntrebari> raspunsuri) {
        List<Integer> puncte = raspunsuri.stream()
                .map(RaspunsuriIntrebari::getScor)
                .filter(Objects::nonNull)
                .toList();

        if (puncte.isEmpty()) {
            return ScorCategorieDTO.builder()
                    .numeCategorie(nume).scorMediu(BigDecimal.ZERO)
                    .interval(ScalaCromatica.VERDE).build();
        }

        double medie = puncte.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        BigDecimal scorMediu = BigDecimal.valueOf(medie).setScale(2, RoundingMode.HALF_UP);

        return ScorCategorieDTO.builder()
                .numeCategorie(nume)
                .scorMediu(scorMediu)
                .numarIntrebari(puncte.size())
                .interval(ScalaCromatica.pentruScor(medie))
                .build();
    }

    private BigDecimal calculeazaScorTotal(List<ScorCategorieDTO> scoruri) {
        if (scoruri.isEmpty()) return BigDecimal.ZERO;
        BigDecimal suma = scoruri.stream()
                .map(ScorCategorieDTO::getScorMediu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(scoruri.size()), 2, RoundingMode.HALF_UP);
    }

    private int getNumarTotalIntrebari(UUID chestionarId) {
        String sql = "SELECT COUNT(i.id) FROM intrebari i " +
                "JOIN categorii_chestionare cc ON i.categorie_id = cc.id " +
                "WHERE cc.chestionar_id = :id";
        try {
            return ((Number) entityManager.createNativeQuery(sql)
                    .setParameter("id", chestionarId)
                    .getSingleResult()).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    // Metodele utilitare rămân neschimbate...
    public Optional<RezultatScoring> getScoruriCalculate(UUID id) { return rezultatScoringRepository.findByRaspunsChestionarId(id); }
    public void stergeScoruri(UUID id) { rezultatScoringRepository.deleteByRaspunsChestionarId(id); }
}
