package com.therapeutica.therapeutica_app.chestionare.ui;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionareRepository;
import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.ChestionareRepository;
import com.therapeutica.therapeutica_app.chestionare.RaportGeneratorService;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import com.therapeutica.therapeutica_app.intrebari.IntrebariRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raport_chestionar.RezultatScoring;
import com.therapeutica.therapeutica_app.raport_chestionar.ScoringService;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chestionare")
@RequiredArgsConstructor
@Slf4j
public class ChestionarController {

    private final ChestionareRepository chestionareRepository;
    private final CategoriiChestionareRepository categoriiChestionareRepository;
    private final IntrebariRepository intrebariRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final RaspunsuriIntrebariService raspunsuriIntrebariService;
    private final RaportGeneratorService raportGeneratorService;
    private final PacientiRepository pacientiRepository;
    private final ScoringService scoringService;


    /**
     * Afișează un chestionar pentru completare
     */
    @GetMapping("/completare/{raspunsChestionarId}")
    public String afiseazaPentruCompletare(@PathVariable UUID raspunsChestionarId,
                                           Model model) {

        log.info("START afiseazaPentruCompletare");
        log.info("Parametru primit: raspunsChestionarId = {}", raspunsChestionarId);

        try {
            RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                    .findByIdForCompletare(raspunsChestionarId)
                    .orElseThrow(() -> {
                        log.error("NU există RaspunsuriChestionare cu ID: {}", raspunsChestionarId);
                        return new NotFoundException("Chestionarul nu a fost găsit în baza de date");
                    });

            log.info("RaspunsChestionar găsit: ID={}, Status={}",
                    raspunsChestionar.getId(), raspunsChestionar.getStatus());

            if (raspunsChestionar.getChestionar() != null) {
                log.info("Chestionar: {}", raspunsChestionar.getChestionar().getNume());
            }

            String sexPacientStr = null;
            if (raspunsChestionar.getPacient() != null) {
                log.info("Pacient ID: {}", raspunsChestionar.getPacient().getId());

                if (raspunsChestionar.getPacient().getUser() != null) {
                    log.info("Pacient User: {} {}",
                            raspunsChestionar.getPacient().getUser().getNume(),
                            raspunsChestionar.getPacient().getUser().getPrenume());

                    if (raspunsChestionar.getPacient().getSex() != null) {
                        sexPacientStr = raspunsChestionar.getPacient().getSex().name();
                        log.info("Sex pacient identificat: {}", sexPacientStr);
                    } else {
                        log.warn("Sexul pacientului nu este completat!");
                    }
                } else {
                    log.warn("Pacientul nu are user asociat!");
                }
            }

            if (raspunsChestionar.getChestionar() == null) {
                log.error("RaspunsuriChestionare nu are chestionar asociat!");
                model.addAttribute("error", "Chestionarul nu are date asociate");
                return "error";
            }

            Chestionare chestionar = raspunsChestionar.getChestionar();

            if (raspunsChestionar.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT) {
                log.info("Chestionar deja completat, redirecționez către rezultate");
                return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
            }

            List<CategoriiChestionare> toateCategoriile = categoriiChestionareRepository
                    .findByChestionarId(chestionar.getId());

            log.info("Găsite {} categorii inițiale", toateCategoriile.size());

            final String finalSexPacient = sexPacientStr;

            List<CategoriiChestionare> categoriiFiltrate = toateCategoriile.stream()
                    .filter(categorie -> {
                        if (categorie.getSexTinta() == null || categorie.getSexTinta().name().equals("AMBELE")) {
                            return true;
                        }
                        if (finalSexPacient == null) {
                            return true;
                        }
                        return categorie.getSexTinta().name().equalsIgnoreCase(finalSexPacient);
                    })
                    .sorted(Comparator.comparingInt(c -> c.getOrdine() != null ? c.getOrdine() : 999))
                    .collect(Collectors.toList());

            log.info("Au rămas {} categorii după filtrarea pe sex", categoriiFiltrate.size());

            Map<UUID, List<Intrebare>> intrebariPeCategorii = new HashMap<>();

            for (CategoriiChestionare categorie : categoriiFiltrate) {
                List<Intrebare> intrebari = intrebariRepository
                        .findByCategorieId(categorie.getId());
                intrebariPeCategorii.put(categorie.getId(), intrebari);

                log.info("Categorie '{}': {} întrebări", categorie.getNume(), intrebari.size());

                for (Intrebare intrebare : intrebari) {
                    log.debug("DEBUG Întrebare: '{}' - Tip: {}",
                            intrebare.getTextIntrebare(),
                            intrebare.getTipIntrebare());
                }
            }

            model.addAttribute("chestionar", chestionar);
            model.addAttribute("raspunsChestionar", raspunsChestionar);
            model.addAttribute("categorii", categoriiFiltrate);
            model.addAttribute("intrebariPeCategorii", intrebariPeCategorii);

            log.info("SUCCESS - Se încarcă template completare");
            return "pacient/completare-chestionar";

        } catch (Exception e) {
            log.error("ERROR în afiseazaPentruCompletare");
            log.error("Mesaj: {}", e.getMessage());
            log.error("Stack trace:", e);

            model.addAttribute("error", "Eroare internă: " + e.getMessage());
            return "error";
        }
    }


    /**
     * Listează chestionarele disponibile pentru un pacient
     */
    @GetMapping("/pacient/{userId}/disponibile")
    public String listeazaDisponibile(@PathVariable UUID userId, Model model) {

        log.info("Afișare chestionare pentru USER: {}", userId);

        try {
            Pacienti pacient = pacientiRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found for user: " + userId));

            UUID pacientId = pacient.getId();
            log.info("Converted user {} to pacient {}", userId, pacientId);

            List<RaspunsuriChestionare> toateChestionare = raspunsuriChestionareRepository
                    .findByPacientIdWithRelations(pacientId);

            List<RaspunsuriChestionare> chestionareDisponibile = toateChestionare.stream()
                    .filter(rc -> rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT)
                    .collect(Collectors.toList());

            List<RaspunsuriChestionare> istoricChestionare = toateChestionare.stream()
                    .filter(rc -> rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT)
                    .sorted((rc1, rc2) -> {
                        if (rc1.getCompletatLa() == null && rc2.getCompletatLa() == null) return 0;
                        if (rc1.getCompletatLa() == null) return 1;
                        if (rc2.getCompletatLa() == null) return -1;
                        return rc2.getCompletatLa().compareTo(rc1.getCompletatLa());
                    })
                    .collect(Collectors.toList());

            model.addAttribute("chestionareDisponibile", chestionareDisponibile);
            model.addAttribute("istoricChestionare", istoricChestionare);
            model.addAttribute("pacientId", pacientId);
            model.addAttribute("activeTab", "disponibile");

            return "pacient/chestionare";

        } catch (Exception e) {
            log.error("Eroare la încărcarea chestionarelor: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare la încărcarea chestionarelor");
            return "pacient/chestionare";
        }
    }

    /**
     * Istoricul chestionarelor completate de un pacient
     */
    @GetMapping("/pacient/{userId}/istoric")
    public String afiseazaIstoric(@PathVariable UUID userId,
                                  Model model) {

        try {
            Pacienti pacient = pacientiRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found for user: " + userId));

            UUID pacientId = pacient.getId();
            log.info("Converted user {} to pacient {}", userId, pacientId);

            List<RaspunsuriChestionare> istoric = raspunsuriChestionareRepository
                    .findByPacientIdAndStatus(pacientId,
                            RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

            istoric.sort((rc1, rc2) -> rc2.getCompletatLa().compareTo(rc1.getCompletatLa()));

            List<RaspunsuriChestionare> chestionareDisponibile = raspunsuriChestionareRepository
                    .findByPacientIdAndStatus(pacientId,
                            RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);

            model.addAttribute("istoricChestionare", istoric);
            model.addAttribute("chestionareDisponibile", chestionareDisponibile);
            model.addAttribute("pacientId", pacientId);
            model.addAttribute("activeTab", "istoric");

            return "pacient/chestionare";

        } catch (Exception e) {
            log.error("Eroare la încărcarea istoricului: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare la încărcarea istoricului");
            return "pacient/chestionare";
        }
    }

    // ========== PENTRU MEDIC

    /**
     * Vizualizează răspunsurile unui pacient (pentru medic)
     */
    @GetMapping("/medic/vizualizare/{raspunsChestionarId}")
    public String vizualizeazaRaspunsuri(@PathVariable UUID raspunsChestionarId,
                                         HttpSession session,
                                         Model model) {

        // findByIdForCompletare face JOIN FETCH pe chestionar, pacient și pacient.user -
        // necesar fiindcă template-ul accesează chestionar.chestionar.nume și
        // potențial chestionar.pacient.user.*, deja în afara tranzacției curente
        RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                .findByIdForCompletare(raspunsChestionarId)
                .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

        // Calculează (sau reutilizează, dacă e deja valid) scorul și interpretările pe categorii
        RezultatScoring rezultat = scoringService.calculeazaSiSalveazaScor(raspunsChestionarId);

        // Obține răspunsurile detaliate (cu fetch eager pe intrebare/categorie)
        List<RaspunsuriIntrebari> raspunsuriDetaliate = raspunsuriIntrebariService
                .getRaspunsuriDetaliate(raspunsChestionarId);

        // Grupează răspunsurile după NUMELE categoriei (nu UUID), fiindcă
        // rezultat.scoruriCategorii / culoriCategorii / interpretariCategorii
        // sunt indexate după numele categoriei, nu după id
        Map<String, List<RaspunsuriIntrebari>> raspunsuriPeCategorii = raspunsuriDetaliate
                .stream()
                .filter(r -> r.getCategorie() != null)
                .collect(Collectors.groupingBy(r -> r.getCategorie().getNume()));

        // Construiește un map nume categorie -> entitate CategoriiChestionare,
        // necesar în template pentru a ști dacă o categorie e evaluabilă (scor)
        // sau doar informativă (context clinic/stil de viață)
        Map<String, CategoriiChestionare> categoriiMap = raspunsuriDetaliate.stream()
                .map(RaspunsuriIntrebari::getCategorie)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        CategoriiChestionare::getNume,
                        c -> c,
                        (c1, c2) -> c1
                ));

        String medicId = (String) session.getAttribute("userId");

        model.addAttribute("chestionar", raspunsChestionar);
        model.addAttribute("rezultat", rezultat);
        model.addAttribute("raspunsuriPeCategorii", raspunsuriPeCategorii);
        model.addAttribute("categoriiMap", categoriiMap);
        model.addAttribute("medicId", medicId);

        return "medic/vizualizare-rezultate";
    }

    /**
     * Generează raport PDF cu vizualizare
     */
    @GetMapping("/raport/visual/{raspunsChestionarId}")
    public String genereazaRaportVisual(@PathVariable UUID raspunsChestionarId,
                                        Model model,
                                        HttpServletResponse response) {

        log.info("Generare raport vizual pentru chestionar: {}", raspunsChestionarId);

        if (!raportGeneratorService.poateGeneraRaport(raspunsChestionarId)) {
            model.addAttribute("error",
                    "Nu se poate genera raportul. Chestionarul nu este completat sau nu are date suficiente.");
            return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
        }

        try {
            Map<String, Object> raportData = raportGeneratorService
                    .genereazaDateRaportVisual(raspunsChestionarId);

            if (raportData.containsKey("error")) {
                model.addAttribute("error", raportData.get("error"));
                return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
            }

            model.addAllAttributes(raportData);

            response.setContentType("application/pdf");

            String pacientNume = (String) raportData.getOrDefault("pacientNumeComplet", "anonim");
            String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "raport-" + pacientNume.replace(" ", "-") + "-" + data + ".pdf";

            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");

            return "chestionare/raport-visual-pdf";

        } catch (Exception e) {
            log.error("Eroare la generarea raportului vizual: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare la generarea raportului vizual: " + e.getMessage());
            return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
        }
    }

    /**
     * Afișează toate tipurile de chestionare disponibile în sistem (pentru medici)
     */
    @GetMapping("/medic/lista-chestionare")
    public String listaChestionareSistem(Model model) {
        log.info("Medic accesează lista globală de chestionare");

        List<Chestionare> lista = chestionareRepository.findAll();

        model.addAttribute("chestionareSistem", lista);
        return "medic/chestionare/lista-chestionare";
    }

    @GetMapping("/medic/vizualizare-structura/{id}")
    public String vizualizareStructuraChestionar(@PathVariable UUID id, Model model) {
        log.info("Medic vizualizează structura chestionarului: {}", id);

        Chestionare chestionar = chestionareRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

        List<CategoriiChestionare> categorii = categoriiChestionareRepository
                .findByChestionarId(id)
                .stream()
                .sorted(Comparator.comparingInt(c -> c.getOrdine() != null ? c.getOrdine() : 999))
                .collect(Collectors.toList());

        Map<UUID, List<Intrebare>> intrebariPeCategorii = new HashMap<>();
        for (CategoriiChestionare categorie : categorii) {
            List<Intrebare> intrebari = intrebariRepository.findByCategorieId(categorie.getId());
            intrebariPeCategorii.put(categorie.getId(), intrebari);
        }

        model.addAttribute("chestionar", chestionar);
        model.addAttribute("categorii", categorii);
        model.addAttribute("intrebariPeCategorii", intrebariPeCategorii);
        model.addAttribute("esteVizualizareMedic", true);

        return "medic/chestionare/vizualizare-chestionar";
    }

    /**
     * Marchează un chestionar ca fiind revizuit de către medic
     */
    @PostMapping("/medic/marcheaza-revizuit/{raspunsChestionarId}")
    public String marcheazaCaRevizuit(@PathVariable UUID raspunsChestionarId,
                                      @RequestParam UUID pacientId,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            RaspunsuriChestionare raspuns = raspunsuriChestionareRepository.findById(raspunsChestionarId)
                    .orElseThrow(() -> new NotFoundException("Răspunsul nu a fost găsit"));

            if (raspuns.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT) {
                raspuns.setStatus(RaspunsuriChestionare.StatusRaspuns.REVIZUIT);
                raspunsuriChestionareRepository.save(raspuns);
                redirectAttributes.addFlashAttribute("success", "Evaluarea a fost marcată ca revizuită.");
            }

            String medicId = (String) session.getAttribute("userId");
            return "redirect:/medic/" + medicId + "/pacient/" + pacientId;

        } catch (Exception e) {
            String medicId = (String) session.getAttribute("userId");
            return "redirect:/medic/" + medicId + "/pacient/" + pacientId;
        }
    }
}