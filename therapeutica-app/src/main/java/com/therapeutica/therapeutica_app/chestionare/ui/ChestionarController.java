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
import com.therapeutica.therapeutica_app.security.AccesChestionarService;
import com.therapeutica.therapeutica_app.chestionare.RevizuireChestionarService;
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
import com.therapeutica.therapeutica_app.security.AccesPacientService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

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
    private final AccesPacientService accesPacientService;
    private final AccesChestionarService accesChestionarService;
    private final RevizuireChestionarService revizuireChestionarService;

    /**
     * Afișează un chestionar pentru completare
     */
    @GetMapping("/completare/{raspunsChestionarId}")
    public String afiseazaPentruCompletare(@PathVariable UUID raspunsChestionarId,
                                           HttpSession session,
                                           Model model) {

        log.info("Accesare chestionar pentru completare: {}", raspunsChestionarId);

        RaspunsuriChestionare raspunsChestionar =
                accesChestionarService.incarcaPentruPacient(
                        session,
                        raspunsChestionarId
                );

        try {
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

            if (raspunsChestionar.getStatus()
                    == RaspunsuriChestionare.StatusRaspuns.COMPLETAT
                    || raspunsChestionar.getStatus()
                    == RaspunsuriChestionare.StatusRaspuns.REVIZUIT) {

                return "redirect:/chestionare/pacient/"
                        + session.getAttribute("userId")
                        + "/istoric";
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

    @GetMapping("/pacient/{userId}/disponibile")
    public String listeazaDisponibile(
            @PathVariable UUID userId,
            HttpSession session,
            Model model) {

        return pregatesteListaPacient(userId, session, model, "disponibile");
    }

    @GetMapping("/pacient/{userId}/istoric")
    public String afiseazaIstoric(
            @PathVariable UUID userId,
            HttpSession session,
            Model model) {

        return pregatesteListaPacient(userId, session, model, "istoric");
    }

    private String pregatesteListaPacient(
            UUID userId,
            HttpSession session,
            Model model,
            String activeTab) {

        accesPacientService.verificaAccesPropriuByUserId(session, userId);

        Pacienti pacient = pacientiRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new NotFoundException("Profilul pacientului nu există."));

        List<RaspunsuriChestionare> disponibile =
                raspunsuriChestionareRepository
                        .findByPacientIdAndStatusFullRelations(
                                pacient.getId(),
                                RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT
                        );

        List<RaspunsuriChestionare> istoric =
                raspunsuriChestionareRepository
                        .findByPacientIdAndStatusInFullRelations(
                                pacient.getId(),
                                List.of(
                                        RaspunsuriChestionare.StatusRaspuns.COMPLETAT,
                                        RaspunsuriChestionare.StatusRaspuns.REVIZUIT
                                )
                        );

        model.addAttribute("chestionareDisponibile", disponibile);
        model.addAttribute("istoricChestionare", istoric);
        model.addAttribute("pacientId", pacient.getId());
        model.addAttribute("userId", userId);
        model.addAttribute("activeTab", activeTab);

        return "pacient/chestionare";
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

        if (pacient == null) {
            throw new NotFoundException("Pacientul asociat nu a fost găsit.");
        }

        accesPacientService.verificaAccesMedic(session, pacient.getId());

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
    public ResponseEntity<byte[]> genereazaRaportVisual(
            @PathVariable UUID raspunsChestionarId,
            HttpSession session) throws IOException {

        RaspunsuriChestionare raspunsChestionar =
                raspunsuriChestionareRepository
                        .findByIdForCompletare(raspunsChestionarId)
                        .orElseThrow(() ->
                                new NotFoundException("Chestionarul nu a fost găsit."));

        if (raspunsChestionar.getPacient() == null) {
            throw new NotFoundException("Pacientul asociat nu a fost găsit.");
        }

        UUID pacientId = raspunsChestionar.getPacient().getId();

        accesPacientService.verificaAccesMedic(session, pacientId);

        byte[] pdf = raportGeneratorService.genereazaPdf(raspunsChestionarId);

        auditService.inregistreaza(
                auditService.utilizatorCurent(session),
                AuditLog.ActiuneAudit.EXPORT,
                AuditLog.TipResursa.CHESTIONAR,
                raspunsChestionarId,
                pacientId
        );

        String numeFisier = "raport-chestionar-" + raspunsChestionarId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(numeFisier)
                                .build()
                                .toString()
                )
                .body(pdf);
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
    public String marcheazaCaRevizuit(
            @PathVariable UUID raspunsChestionarId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        RevizuireChestionarService.RezultatRevizuire rezultat =
                revizuireChestionarService.marcheazaCaRevizuit(
                        raspunsChestionarId,
                        session
                );

        if (rezultat.modificat()) {
            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.MODIFICARE,
                    AuditLog.TipResursa.CHESTIONAR,
                    raspunsChestionarId,
                    rezultat.pacientId()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Evaluarea a fost marcată ca revizuită."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Evaluarea era deja marcată ca revizuită."
            );
        }

        return "redirect:/medic/"
                + session.getAttribute("userId")
                + "/pacient/"
                + rezultat.pacientId();
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