package com.therapeutica.therapeutica_app.chestionare.ui;

import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
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
    private final AuditService auditService;

    /**
     * Afișează un chestionar pentru completare
     */
    @GetMapping("/completare/{raspunsChestionarId}")
    public String afiseazaPentruCompletare(@PathVariable UUID raspunsChestionarId,
                                           HttpSession session,
                                           Model model) {

        log.info("Accesare chestionar pentru completare: {}", raspunsChestionarId);

        try {
            RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                    .findByIdForCompletare(raspunsChestionarId)
                    .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit în baza de date"));

            if (raspunsChestionar.getChestionar() == null) {
                log.error("RaspunsuriChestionare {} nu are chestionar asociat!", raspunsChestionarId);
                model.addAttribute("error", "Chestionarul nu are date asociate");
                return "error";
            }

            Pacienti pacient = raspunsChestionar.getPacient();

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.VIZUALIZARE,
                    AuditLog.TipResursa.CHESTIONAR,
                    raspunsChestionarId,
                    pacient != null ? pacient.getId() : null
            );

            if (raspunsChestionar.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT) {
                log.info("Chestionar deja completat, redirecționez către rezultate");
                return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
            }

            // Sexul pacientului determină ce categorii sunt afișate
            String sexPacient = null;
            if (pacient != null && pacient.getSex() != null) {
                sexPacient = pacient.getSex().name();
            }

            Chestionare chestionar = raspunsChestionar.getChestionar();

            List<CategoriiChestionare> toateCategoriile = categoriiChestionareRepository
                    .findByChestionarId(chestionar.getId());

            List<CategoriiChestionare> categoriiFiltrate = filtreazaCategoriiDupaSex(toateCategoriile, sexPacient);

            log.info("Chestionar '{}': {} categorii din {} după filtrarea pe sex",
                    chestionar.getNume(), categoriiFiltrate.size(), toateCategoriile.size());

            Map<UUID, List<Intrebare>> intrebariPeCategorii = new LinkedHashMap<>();
            for (CategoriiChestionare categorie : categoriiFiltrate) {
                List<Intrebare> intrebari = intrebariRepository.findByCategorieId(categorie.getId());
                intrebariPeCategorii.put(categorie.getId(), intrebari);
            }

            model.addAttribute("chestionar", chestionar);
            model.addAttribute("raspunsChestionar", raspunsChestionar);
            model.addAttribute("categorii", categoriiFiltrate);
            model.addAttribute("intrebariPeCategorii", intrebariPeCategorii);

            return "pacient/completare-chestionar";

        } catch (Exception e) {
            log.error("Eroare la afișarea chestionarului {}: {}", raspunsChestionarId, e.getMessage(), e);
            model.addAttribute("error", "Eroare internă: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Listează chestionarele disponibile pentru un pacient
     */
    @GetMapping("/pacient/{userId}/disponibile")
    public String listeazaDisponibile(@PathVariable UUID userId, Model model) {

        log.info("Afișare chestionare pentru user: {}", userId);

        try {
            Pacienti pacient = pacientiRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found for user: " + userId));

            UUID pacientId = pacient.getId();

            List<RaspunsuriChestionare> toateChestionare = raspunsuriChestionareRepository
                    .findByPacientIdWithRelations(pacientId);

            List<RaspunsuriChestionare> chestionareDisponibile = new ArrayList<>();
            List<RaspunsuriChestionare> istoricChestionare = new ArrayList<>();

            for (RaspunsuriChestionare rc : toateChestionare) {
                if (rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT) {
                    chestionareDisponibile.add(rc);
                } else if (rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT) {
                    istoricChestionare.add(rc);
                }
            }

            istoricChestionare.sort(comparatorDupaDataCompletare());

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
    public String afiseazaIstoric(@PathVariable UUID userId, Model model) {

        try {
            Pacienti pacient = pacientiRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found for user: " + userId));

            UUID pacientId = pacient.getId();

            List<RaspunsuriChestionare> istoric = raspunsuriChestionareRepository
                    .findByPacientIdAndStatus(pacientId, RaspunsuriChestionare.StatusRaspuns.COMPLETAT);

            istoric.sort(comparatorDupaDataCompletare());

            List<RaspunsuriChestionare> chestionareDisponibile = raspunsuriChestionareRepository
                    .findByPacientIdAndStatus(pacientId, RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);

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

    // ========== PENTRU MEDIC ==========

    /**
     * Vizualizează răspunsurile unui pacient (pentru medic).
     * Rută unică pentru afișarea rezultatelor — folosită atât din profilul
     * pacientului, cât și din notificarea de chestionar completat.
     */
    @GetMapping("/medic/vizualizare/{raspunsChestionarId}")
    public String vizualizeazaRaspunsuri(@PathVariable UUID raspunsChestionarId,
                                         HttpSession session,
                                         Model model) {

        // findByIdForCompletare face JOIN FETCH pe chestionar, pacient și pacient.user —
        // necesar fiindcă template-ul accesează aceste relații în afara tranzacției
        RaspunsuriChestionare raspunsChestionar = raspunsuriChestionareRepository
                .findByIdForCompletare(raspunsChestionarId)
                .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

        Pacienti pacient = raspunsChestionar.getPacient();

        auditService.inregistreaza(
                auditService.utilizatorCurent(session),
                AuditLog.ActiuneAudit.VIZUALIZARE,
                AuditLog.TipResursa.CHESTIONAR,
                raspunsChestionarId,
                pacient != null ? pacient.getId() : null
        );

        // Calculează (sau reutilizează, dacă e deja valid) scorul pe categorii
        RezultatScoring rezultat = scoringService.calculeazaSiSalveazaScor(raspunsChestionarId);

        List<RaspunsuriIntrebari> raspunsuriDetaliate = raspunsuriIntrebariService
                .getRaspunsuriDetaliate(raspunsChestionarId);

        // Grupare după NUMELE categoriei (nu UUID), fiindcă rezultat.scoruriCategorii
        // și celelalte map-uri din RezultatScoring sunt indexate după nume.
        // LinkedHashMap păstrează ordinea categoriilor din chestionar.
        Map<String, List<RaspunsuriIntrebari>> raspunsuriPeCategorii = new LinkedHashMap<>();
        for (RaspunsuriIntrebari raspuns : raspunsuriDetaliate) {
            String numeCategorie = (raspuns.getCategorie() != null)
                    ? raspuns.getCategorie().getNume()
                    : "Fără Categorie";

            raspunsuriPeCategorii
                    .computeIfAbsent(numeCategorie, k -> new ArrayList<>())
                    .add(raspuns);
        }

        // Map nume categorie -> entitate, necesar în template pentru a distinge
        // categoriile evaluabile (cu scor) de cele informative
        Map<String, CategoriiChestionare> categoriiMap = new LinkedHashMap<>();
        for (RaspunsuriIntrebari raspuns : raspunsuriDetaliate) {
            CategoriiChestionare categorie = raspuns.getCategorie();
            if (categorie != null) {
                categoriiMap.putIfAbsent(categorie.getNume(), categorie);
            }
        }

        model.addAttribute("chestionar", raspunsChestionar);
        model.addAttribute("rezultat", rezultat);
        model.addAttribute("raspunsuriPeCategorii", raspunsuriPeCategorii);
        model.addAttribute("categoriiMap", categoriiMap);
        model.addAttribute("medicId", session.getAttribute("userId"));

        return "medic/vizualizare-rezultate";
    }

    /**
     * Generează raport PDF cu vizualizare
     */
    @GetMapping("/raport/visual/{raspunsChestionarId}")
    public String genereazaRaportVisual(@PathVariable UUID raspunsChestionarId,
                                        HttpSession session,
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

            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.EXPORT,
                    AuditLog.TipResursa.CHESTIONAR,
                    raspunsChestionarId,
                    extragePacientId(raspunsChestionarId)
            );

            model.addAllAttributes(raportData);

            response.setContentType("application/pdf");

            String pacientNume = (String) raportData.getOrDefault("pacientNumeComplet", "anonim");
            String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "raport-" + pacientNume.replace(" ", "-") + "-" + data + ".pdf";

            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            return "chestionare/raport-visual-pdf";

        } catch (Exception e) {
            log.error("Eroare la generarea raportului vizual: {}", e.getMessage(), e);
            model.addAttribute("error", "Eroare la generarea raportului vizual: " + e.getMessage());
            return "redirect:/chestionare/rezultate/" + raspunsChestionarId;
        }
    }

    /**
     * Afișează toate tipurile de chestionare disponibile în sistem (pentru medici).
     * Nu conține date de pacient, deci nu se auditează.
     */
    @GetMapping("/medic/lista-chestionare")
    public String listaChestionareSistem(Model model) {
        log.info("Medic accesează lista globală de chestionare");

        model.addAttribute("chestionareSistem", chestionareRepository.findAll());
        return "medic/chestionare/lista-chestionare";
    }

    /**
     * Vizualizează structura unui chestionar (șablon, fără date de pacient).
     * Nu se auditează.
     */
    @GetMapping("/medic/vizualizare-structura/{id}")
    public String vizualizareStructuraChestionar(@PathVariable UUID id, Model model) {
        log.info("Medic vizualizează structura chestionarului: {}", id);

        Chestionare chestionar = chestionareRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chestionarul nu a fost găsit"));

        List<CategoriiChestionare> categorii = categoriiChestionareRepository.findByChestionarId(id);
        categorii.sort(comparatorDupaOrdine());

        Map<UUID, List<Intrebare>> intrebariPeCategorii = new LinkedHashMap<>();
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

        Object medicId = session.getAttribute("userId");

        try {
            RaspunsuriChestionare raspuns = raspunsuriChestionareRepository.findById(raspunsChestionarId)
                    .orElseThrow(() -> new NotFoundException("Răspunsul nu a fost găsit"));

            if (raspuns.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT) {
                raspuns.setStatus(RaspunsuriChestionare.StatusRaspuns.REVIZUIT);
                raspunsuriChestionareRepository.save(raspuns);

                auditService.inregistreaza(
                        auditService.utilizatorCurent(session),
                        AuditLog.ActiuneAudit.MODIFICARE,
                        AuditLog.TipResursa.CHESTIONAR,
                        raspunsChestionarId,
                        pacientId
                );

                redirectAttributes.addFlashAttribute("success", "Evaluarea a fost marcată ca revizuită.");
            }

        } catch (Exception e) {
            log.error("Eroare la marcarea ca revizuit a chestionarului {}: {}",
                    raspunsChestionarId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nu s-a putut marca evaluarea ca revizuită.");
        }

        return "redirect:/medic/" + medicId + "/pacient/" + pacientId;
    }

    // ========== METODE HELPER ==========

    /**
     * Păstrează categoriile potrivite pentru sexul pacientului.
     * Categoriile fără sex-țintă sau marcate AMBELE se aplică tuturor.
     */
    private List<CategoriiChestionare> filtreazaCategoriiDupaSex(List<CategoriiChestionare> categorii,
                                                                 String sexPacient) {
        List<CategoriiChestionare> rezultat = new ArrayList<>();

        for (CategoriiChestionare categorie : categorii) {
            if (categorie.getSexTinta() == null
                    || "AMBELE".equals(categorie.getSexTinta().name())
                    || sexPacient == null
                    || categorie.getSexTinta().name().equalsIgnoreCase(sexPacient)) {
                rezultat.add(categorie);
            }
        }

        rezultat.sort(comparatorDupaOrdine());
        return rezultat;
    }

    private Comparator<CategoriiChestionare> comparatorDupaOrdine() {
        return Comparator.comparingInt(c -> c.getOrdine() != null ? c.getOrdine() : 999);
    }

    /**
     * Cel mai recent completat primul; cele fără dată la final.
     */
    private Comparator<RaspunsuriChestionare> comparatorDupaDataCompletare() {
        return (rc1, rc2) -> {
            if (rc1.getCompletatLa() == null && rc2.getCompletatLa() == null) return 0;
            if (rc1.getCompletatLa() == null) return 1;
            if (rc2.getCompletatLa() == null) return -1;
            return rc2.getCompletatLa().compareTo(rc1.getCompletatLa());
        };
    }

    private UUID extragePacientId(UUID raspunsChestionarId) {
        return raspunsuriChestionareRepository.findById(raspunsChestionarId)
                .map(rc -> rc.getPacient() != null ? rc.getPacient().getId() : null)
                .orElse(null);
    }
}