package com.therapeutica.therapeutica_app.chestionare;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionareRepository;
import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.dto.CategorieVisualDTO;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import com.therapeutica.therapeutica_app.intrebari.IntrebariRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class RaportGeneratorService {

    private final RaspunsuriIntrebariService raspunsuriIntrebariService;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final CategoriiChestionareRepository categoriiChestionareRepository;
    private final IntrebariRepository intrebariRepository;

    /**
     * Generează date pentru raport cu vizualizarea barelor colorate
     */
    public Map<String, Object> genereazaDateRaportVisual(UUID raspunsChestionarId) {
        log.info("🎨 Generare date raport vizual pentru chestionar: {}", raspunsChestionarId);

        Map<String, Object> raportData = new HashMap<>();

        try {
            // Obține înregistrarea principală
            RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                    .findById(raspunsChestionarId)
                    .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

            // Obține analiza
            Map<String, Object> analiza = raspunsuriIntrebariService
                    .analizeazaRaspunsuri(raspunsChestionarId);

            // Obține detalii chestionar
            Chestionare chestionar = raspunsChestionar.getChestionar();
            List<CategoriiChestionare> categorii = categoriiChestionareRepository
                    .findByChestionarId(chestionar.getId());

            // Obține răspunsurile
            List<RaspunsuriIntrebari> raspunsuri = raspunsuriIntrebariService
                    .getRaspunsuriDetaliate(raspunsChestionarId);

            // Generează vizualizarea pentru categorii (bare colorate)
            List<CategorieVisualDTO> categoriiVisual = genereazaVizualizareCategorii(
                    categorii, raspunsuri, analiza);

            // Grupează pentru layout (2 coloane)
            Map<String, List<CategorieVisualDTO>> categoriiPeColoane =
                    grupeazaPeColoane(categoriiVisual);

            // Calculează statistici
            Map<String, Object> statistici = calculeazaStatisticiVizuale(
                    categoriiVisual, raspunsuri);

            // Generează paleta de culori pentru scoruri
            Map<Integer, String> paletaCulori = genereazaPaletaCulori();

            // Adaugă datele în map
            raportData.put("chestionar", chestionar);
            raportData.put("raspunsChestionar", raspunsChestionar);
            raportData.put("categoriiVisual", categoriiVisual);
            raportData.put("categoriiPeColoane", categoriiPeColoane);
            raportData.put("paletaCulori", paletaCulori);
            raportData.put("statistici", statistici);
            raportData.put("dataGenerare", LocalDateTime.now());
            raportData.put("titluRaport", "REZULTATE CHESTIONAR: " + chestionar.getNume());

            // Informații pacient
            if (raspunsChestionar.getPacient() != null) {
                raportData.put("pacientNumeComplet",
                        formatNumeComplet(raspunsChestionar.getPacient()));
//                raportData.put("pacientVarsta",
//                        calculeazaVarsta(raspunsChestionar.getPacient().getDataNasterii()));
//                raportData.put("pacientGen",
//                        raspunsChestionar.getPacient().getGen());
            }

            // Informații medic
            if (raspunsChestionar.getMedic() != null) {
                raportData.put("medicNumeComplet",
                        formatNumeComplet(raspunsChestionar.getMedic()));
                raportData.put("medicSpecialitate",
                        raspunsChestionar.getMedic().getSpecializare());
            }

            log.info("✅ Date raport vizual generate: {} categorii", categoriiVisual.size());

        } catch (Exception e) {
            log.error("❌ Eroare la generarea raportului vizual: {}", e.getMessage(), e);
            raportData.put("error", "Eroare la generarea raportului: " + e.getMessage());
        }

        return raportData;
    }

    /**
     * Generează vizualizarea cu bare colorate pentru categorii
     */
    private List<CategorieVisualDTO> genereazaVizualizareCategorii(
            List<CategoriiChestionare> categorii,
            List<RaspunsuriIntrebari> raspunsuri,
            Map<String, Object> analiza) {

        List<CategorieVisualDTO> categoriiVisual = new ArrayList<>();

        // Grupează răspunsurile pe categorii
        Map<UUID, List<RaspunsuriIntrebari>> raspunsuriPeCategorii = raspunsuri.stream()
                .filter(r -> r.getCategorie() != null)
                .collect(Collectors.groupingBy(r -> r.getCategorie().getId()));

        // Extrage scorurile din analiză
        @SuppressWarnings("unchecked")
        Map<String, Double> scoruriCategorii = (Map<String, Double>) analiza.get("scoruriCategorii");

        @SuppressWarnings("unchecked")
        Map<String, String> interpretariCategorii = (Map<String, String>) analiza.get("interpretariCategorii");

        for (CategoriiChestionare categorie : categorii) {
            // Obține scorul pentru această categorie
            Double scorMediu = scoruriCategorii.getOrDefault(categorie.getNume(), 0.0);
            String interpretare = interpretariCategorii.getOrDefault(categorie.getNume(), "Normal");

            // Calculează procentajul (0-100) pentru bara
            // Presupunem că scorul maxim este 5 (ajustabil)
            int scorMaxim = 5;
            int procentaj = (int) Math.round((scorMediu / scorMaxim) * 100);

            // Determină culoarea în funcție de scor
            String culoare = determinaCuloare(scorMediu);

            // Numără întrebările din această categorie
            int numarIntrebari = raspunsuriPeCategorii.getOrDefault(categorie.getId(),
                    Collections.emptyList()).size();

            // Creează DTO-ul vizual
            CategorieVisualDTO dto = new CategorieVisualDTO();
            dto.setId(categorie.getId());
            dto.setNume(categorie.getNume());
            dto.setScorMediu(scorMediu);
            dto.setScorMaxim(scorMaxim);
            dto.setProcentaj(procentaj);
            dto.setCuloare(culoare);
            dto.setInterpretare(interpretare);
            dto.setNumarIntrebari(numarIntrebari);

            categoriiVisual.add(dto);
        }

        // Sortează după scor (cele mai mari scoruri primele)
        categoriiVisual.sort((c1, c2) -> Double.compare(c2.getScorMediu(), c1.getScorMediu()));

        return categoriiVisual;
    }

    /**
     * Determină culoarea în funcție de scor
     */
    private String determinaCuloare(Double scor) {
        if (scor >= 4.0) return "#FF6B6B"; // Roșu - sever
        else if (scor >= 3.0) return "#FFA726"; // Portocaliu - ridicat
        else if (scor >= 2.0) return "#FFD166"; // Galben - moderat
        else if (scor >= 1.0) return "#06D6A0"; // Verde deschis - ușor
        else return "#118AB2"; // Albastru - normal
    }

    /**
     * Grupează categoriile pe 2 coloane pentru layout
     */
    private Map<String, List<CategorieVisualDTO>> grupeazaPeColoane(
            List<CategorieVisualDTO> categorii) {

        Map<String, List<CategorieVisualDTO>> coloane = new HashMap<>();

        int mijloc = (int) Math.ceil(categorii.size() / 2.0);

        coloane.put("coloanaStanga", categorii.subList(0, mijloc));
        coloane.put("coloanaDreapta", categorii.subList(mijloc, categorii.size()));

        return coloane;
    }

    /**
     * Generează paleta de culori pentru scoruri
     */
    private Map<Integer, String> genereazaPaletaCulori() {
        Map<Integer, String> paleta = new HashMap<>();

        // Culori pentru diferite scoruri (0-5)
        paleta.put(0, "#118AB2"); // Albastru - normal
        paleta.put(1, "#06D6A0"); // Verde - ușor
        paleta.put(2, "#FFD166"); // Galben - moderat
        paleta.put(3, "#FFA726"); // Portocaliu - ridicat
        paleta.put(4, "#FF6B6B"); // Roșu - sever
        paleta.put(5, "#EF476F"); // Roșu închis - foarte sever

        return paleta;
    }

    public boolean poateGeneraRaport(UUID raspunsChestionarId) {
        try {
            RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                    .findById(raspunsChestionarId)
                    .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

            // Verifică dacă chestionarul este completat
            boolean esteCompletat = raspunsChestionar.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT;

            // Verifică dacă există răspunsuri
            List<RaspunsuriIntrebari> raspunsuri = raspunsuriIntrebariService
                    .getRaspunsuriDetaliate(raspunsChestionarId);
            boolean areRaspunsuri = !raspunsuri.isEmpty();

            // Verifică dacă există categorii
            Chestionare chestionar = raspunsChestionar.getChestionar();
            List<CategoriiChestionare> categorii = categoriiChestionareRepository
                    .findByChestionarId(chestionar.getId());
            boolean areCategorii = !categorii.isEmpty();

            return esteCompletat && areRaspunsuri && areCategorii;

        } catch (Exception e) {
            log.error("❌ Eroare la verificarea capacității de generare raport: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calculează statistici pentru vizualizare
     */
    private Map<String, Object> calculeazaStatisticiVizuale(
            List<CategorieVisualDTO> categoriiVisual,
            List<RaspunsuriIntrebari> raspunsuri) {

        Map<String, Object> statistici = new HashMap<>();

        // Scorul mediu general
        double scorMediuGeneral = categoriiVisual.stream()
                .mapToDouble(CategorieVisualDTO::getScorMediu)
                .average()
                .orElse(0.0);

        // Categoria cu cel mai mare scor
        CategorieVisualDTO categorieMax = categoriiVisual.stream()
                .max(Comparator.comparingDouble(CategorieVisualDTO::getScorMediu))
                .orElse(null);

        // Categoria cu cel mai mic scor
        CategorieVisualDTO categorieMin = categoriiVisual.stream()
                .min(Comparator.comparingDouble(CategorieVisualDTO::getScorMediu))
                .orElse(null);

        // Număr de categorii cu scor ridicat (>3)
        long categoriiRidicat = categoriiVisual.stream()
                .filter(c -> c.getScorMediu() >= 3.0)
                .count();

        // Număr de categorii cu scor moderat (2-3)
        long categoriiModerat = categoriiVisual.stream()
                .filter(c -> c.getScorMediu() >= 2.0 && c.getScorMediu() < 3.0)
                .count();

        statistici.put("scorMediuGeneral", Math.round(scorMediuGeneral * 100.0) / 100.0);
        statistici.put("categorieMax", categorieMax);
        statistici.put("categorieMin", categorieMin);
        statistici.put("categoriiRidicat", categoriiRidicat);
        statistici.put("categoriiModerat", categoriiModerat);
        statistici.put("totalCategorii", categoriiVisual.size());
        statistici.put("totalIntrebari", raspunsuri.size());

        return statistici;
    }

    /**
     * Calculează vârsta
     */
    private Integer calculeazaVarsta(LocalDate dataNasterii) {
        if (dataNasterii == null) return null;
        return Period.between(dataNasterii, LocalDate.now()).getYears();
    }

    /**
     * Formatează nume complet
     */
    private String formatNumeComplet(Object persoana) {
        // Implementare specifică în funcție de structura ta
        return "Nume Persoană";
    }




}