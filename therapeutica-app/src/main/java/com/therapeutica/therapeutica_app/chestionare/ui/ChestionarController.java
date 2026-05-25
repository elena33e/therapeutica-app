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


    /**
     * Afișează un chestionar pentru completare
     */
    @GetMapping("/completare/{raspunsChestionarId}")
    public String afiseazaPentruCompletare(@PathVariable UUID raspunsChestionarId,
                                           Model model) {

        log.info("START afiseazaPentruCompletare");
        log.info("Parametru primit: raspunsChestionarId = {}", raspunsChestionarId);

        try {
            // 1. Folosește metoda cu TOATE relațiile
            log.info("Caută RaspunsuriChestionare cu toate relațiile...");

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

            // 2. Extragem detaliile pacientului și sexul acestuia
            String sexPacientStr = null;
            if (raspunsChestionar.getPacient() != null) {
                log.info("Pacient ID: {}", raspunsChestionar.getPacient().getId());

                if (raspunsChestionar.getPacient().getUser() != null) {
                    log.info("Pacient User: {} {}",
                            raspunsChestionar.getPacient().getUser().getNume(),
                            raspunsChestionar.getPacient().getUser().getPrenume());

                    // Preluăm sexul pacientului (dacă există)
                    if (raspunsChestionar.getPacient().getSex() != null) {
                        sexPacientStr = raspunsChestionar.getPacient().getSex().name();
                        log.info("Sex pacient identificat: {}", sexPacientStr);
                    } else {
                        log.warn("Sexul pacientului nu este completat!");
                    }
                } else {
                    log.warn("⚠ Pacientul nu are user asociat!");
                }
            }

            // 3. Verifică chestionarul asociat
            if (raspunsChestionar.getChestionar() == null) {
                log.error("RaspunsuriChestionare nu are chestionar asociat!");
                model.addAttribute("error", "Chestionarul nu are date asociate");
                return "error";
            }

            Chestionare chestionar = raspunsChestionar.getChestionar();

            // 4. Verifică statusul
            if (raspunsChestionar.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT) {
                log.info("Chestionar deja completat, redirecționez către rezultate");
                return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
            }

            // 5. Obține și filtrează categoriile pe baza sexului
            List<CategoriiChestionare> toateCategoriile = categoriiChestionareRepository
                    .findByChestionarId(chestionar.getId());

            log.info("Găsite {} categorii inițiale", toateCategoriile.size());

            final String finalSexPacient = sexPacientStr;

            List<CategoriiChestionare> categoriiFiltrate = toateCategoriile.stream()
                    .filter(categorie -> {
                        // Dacă categoria e generală (AMBELE sau null), o păstrăm
                        if (categorie.getSexTinta() == null || categorie.getSexTinta().name().equals("AMBELE")) {
                            return true;
                        }
                        // Dacă nu cunoaștem sexul pacientului, afișăm tot din siguranță
                        if (finalSexPacient == null) {
                            return true;
                        }
                        // Păstrăm categoria doar dacă e destinată sexului pacientului
                        return categorie.getSexTinta().name().equalsIgnoreCase(finalSexPacient);
                    })
                    .sorted(Comparator.comparingInt(c -> c.getOrdine() != null ? c.getOrdine() : 999))
                    .collect(Collectors.toList());

            log.info("Au rămas {} categorii după filtrarea pe sex", categoriiFiltrate.size());

            // 6. Obține întrebările doar pentru categoriile filtrate
            Map<UUID, List<Intrebare>> intrebariPeCategorii = new HashMap<>();

            for (CategoriiChestionare categorie : categoriiFiltrate) {
                List<Intrebare> intrebari = intrebariRepository
                        .findByCategorieId(categorie.getId());
                intrebariPeCategorii.put(categorie.getId(), intrebari);

                log.info("  ├─ Categorie '{}': {} întrebări", categorie.getNume(), intrebari.size());

                // Debugging avansat (opțional)
                for (Intrebare intrebare : intrebari) {
                    log.debug("DEBUG Întrebare: '{}' - Tip: {}",
                            intrebare.getTextIntrebare(),
                            intrebare.getTipIntrebare());
                }
            }

            // 7. Adaugă datele filtrate la model
            model.addAttribute("chestionar", chestionar);
            model.addAttribute("raspunsChestionar", raspunsChestionar);
            model.addAttribute("categorii", categoriiFiltrate);
            model.addAttribute("intrebariPeCategorii", intrebariPeCategorii);

            log.info("========== SUCCESS - Se încarcă template completare ==========");
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
            // 1. CONVERTEȘTE userId → pacientId
            Pacienti pacient = pacientiRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found for user: " + userId));

            UUID pacientId = pacient.getId();
            log.info("Converted user {} to pacient {}", userId, pacientId);

            // 2. Folosiți metoda cu JOIN FETCH și filtrați în Java
            List<RaspunsuriChestionare> toateChestionare = raspunsuriChestionareRepository
                    .findByPacientIdWithRelations(pacientId);

            // 3. Filtrați manual pentru cele disponibile (NECOMPLETAT)
            List<RaspunsuriChestionare> chestionareDisponibile = toateChestionare.stream()
                    .filter(rc -> rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT)
                    .collect(Collectors.toList());

            // 4. Filtrați pentru istoric (COMPLETAT)
            List<RaspunsuriChestionare> istoricChestionare = toateChestionare.stream()
                    .filter(rc -> rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT)
                    .sorted((rc1, rc2) -> {
                        // Sortează descendent după dată completare
                        if (rc1.getCompletatLa() == null && rc2.getCompletatLa() == null) return 0;
                        if (rc1.getCompletatLa() == null) return 1;
                        if (rc2.getCompletatLa() == null) return -1;
                        return rc2.getCompletatLa().compareTo(rc1.getCompletatLa());
                    })
                    .collect(Collectors.toList());


            // 5. Adaugă la model
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
    public String afiseazaIstoric(@PathVariable UUID userId, // SCHIMBAT numele parametrului
                                  Model model) {

        try {
            // 1. CONVERTEȘTE userId → pacientId
            Pacienti pacient = pacientiRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found for user: " + userId));

            UUID pacientId = pacient.getId();
            log.info("Converted user {} to pacient {}", userId, pacientId);

            // 2. Obține istoricul cu pacientId
            List<RaspunsuriChestionare> istoric = raspunsuriChestionareRepository
                    .findByPacientIdAndStatus(pacientId,  // FOLOSEȘTE pacientId aici!
                            RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

            // 3. Sortează
            istoric.sort((rc1, rc2) -> rc2.getCompletatLa().compareTo(rc1.getCompletatLa()));

            // 4. Obține chestionarele disponibile cu pacientId
            List<RaspunsuriChestionare> chestionareDisponibile = raspunsuriChestionareRepository
                    .findByPacientIdAndStatus(pacientId,  // FOLOSEȘTE pacientId aici!
                            RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);

            // 5. Adaugă la model
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
                                         Model model) {

        RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                .findById(raspunsChestionarId)
                .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));



        // Obține răspunsurile detaliate
        List<RaspunsuriIntrebari> raspunsuriDetaliate = raspunsuriIntrebariService
                .getRaspunsuriDetaliate(raspunsChestionarId);

        // Grupează răspunsurile pe categorii
        Map<UUID, List<RaspunsuriIntrebari>> raspunsuriPeCategorii = raspunsuriDetaliate
                .stream()
                .collect(Collectors.groupingBy(r -> r.getCategorie().getId()));

        model.addAttribute("chestionar", raspunsChestionar.getChestionar());
        model.addAttribute("raspunsChestionar", raspunsChestionar);
        model.addAttribute("raspunsuriDetaliate", raspunsuriDetaliate);
        model.addAttribute("raspunsuriPeCategorii", raspunsuriPeCategorii);

        return "chestionare/vizualizare-medic";
    }

    /**
     * Generează raport PDF cu vizualizare
     */
    @GetMapping("/raport/visual/{raspunsChestionarId}")
    public String genereazaRaportVisual(@PathVariable UUID raspunsChestionarId,
                                        Model model,
                                        HttpServletResponse response) {

        log.info("Generare raport vizual pentru chestionar: {}", raspunsChestionarId);

        // Verifică dacă poate fi generat raportul
        if (!raportGeneratorService.poateGeneraRaport(raspunsChestionarId)) {
            model.addAttribute("error",
                    "Nu se poate genera raportul. Chestionarul nu este completat sau nu are date suficiente.");
            return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
        }

        try {
            // Obține datele pentru raport vizual
            Map<String, Object> raportData = raportGeneratorService
                    .genereazaDateRaportVisual(raspunsChestionarId);

            if (raportData.containsKey("error")) {
                model.addAttribute("error", raportData.get("error"));
                return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
            }

            model.addAllAttributes(raportData);

            // Setează pentru PDF export
            response.setContentType("application/pdf");

            // Generează nume de fișier
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

        // Această listă trebuie să fie de tip List<Chestionare>
        List<Chestionare> lista = chestionareRepository.findAll();

        model.addAttribute("chestionareSistem", lista);
        return "medic/chestionare/lista-chestionare";
    }

    @GetMapping("/medic/vizualizare-structura/{id}")
    public String vizualizareStructuraChestionar(@PathVariable UUID id, Model model) {
        log.info("Medic vizualizează structura chestionarului: {}", id);

        Chestionare chestionar = chestionareRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

        // Obținem categoriile (secțiunile de disfuncție)
        List<CategoriiChestionare> categorii = categoriiChestionareRepository
                .findByChestionarId(id)
                .stream()
                .sorted(Comparator.comparingInt(c -> c.getOrdine() != null ? c.getOrdine() : 999))
                .collect(Collectors.toList());

        // Obținem întrebările grupate pe categorii
        Map<UUID, List<Intrebare>> intrebariPeCategorii = new HashMap<>();
        for (CategoriiChestionare categorie : categorii) {
            List<Intrebare> intrebari = intrebariRepository.findByCategorieId(categorie.getId());
            intrebariPeCategorii.put(categorie.getId(), intrebari);
        }

        model.addAttribute("chestionar", chestionar);
        model.addAttribute("categorii", categorii);
        model.addAttribute("intrebariPeCategorii", intrebariPeCategorii);
        model.addAttribute("esteVizualizareMedic", true); // Flag pentru template

        return "medic/chestionare/vizualizare-chestionar";
    }

    /**
     * Marchează un chestionar ca fiind revizuit de către medic
     */
    @PostMapping("/medic/marcheaza-revizuit/{raspunsChestionarId}")
    public String marcheazaCaRevizuit(@PathVariable UUID raspunsChestionarId,
                                      @RequestParam UUID pacientId,
                                      HttpSession session, // Adaugă sesiunea aici
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

