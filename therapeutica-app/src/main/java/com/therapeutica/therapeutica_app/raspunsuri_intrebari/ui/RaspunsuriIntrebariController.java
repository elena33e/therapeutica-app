package com.therapeutica.therapeutica_app.raspunsuri_intrebari.ui;

import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariService;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.SalvareChestionarService;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.ValidareRaspunsuriService;
import com.therapeutica.therapeutica_app.security.AccesChestionarService;
import com.therapeutica.therapeutica_app.util.AccesInterzisException;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/raspunsuri")
@RequiredArgsConstructor
@Slf4j
public class RaspunsuriIntrebariController {

    private final RaspunsuriIntrebariService raspunsuriIntrebariService;
    private final SalvareChestionarService salvareChestionarService;
    private final AuditService auditService;
    private final AccesChestionarService accesChestionarService;
    private final ValidareRaspunsuriService validareRaspunsuriService;

    @PostMapping("/completare/{raspunsChestionarId}/salveaza")
    public String salveazaRaspunsuri(
            @PathVariable UUID raspunsChestionarId,
            @RequestParam MultiValueMap<String, String> raspunsuri,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        accesChestionarService.verificaModificare(
                session,
                raspunsChestionarId
        );

        var validare = validareRaspunsuriService.valideaza(
                raspunsChestionarId,
                raspunsuri
        );

        if (!validare.erori().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "eroriValidare",
                    validare.erori()
            );

            redirectAttributes.addFlashAttribute(
                    "raspunsuriFormular",
                    extrageRaspunsuriFormular(raspunsuri)
            );

            return "redirect:/chestionare/completare/"
                    + raspunsChestionarId;
        }

        SalvareChestionarService.RezultatSalvare rezultat;

        try {
            rezultat = salvareChestionarService.salveaza(
                    raspunsChestionarId,
                    validare.raspunsuri(),
                    session
            );
        } catch (AccesInterzisException
                 | NotFoundException
                 | ResponseStatusException e) {

            // Păstrăm răspunsurile HTTP pentru acces, lipsă și conflict.
            throw e;

        } catch (Exception e) {
            log.error(
                    "Eroare la salvarea chestionarului {}",
                    raspunsChestionarId,
                    e
            );

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Chestionarul nu a putut fi salvat. "
                            + "Te rugăm să încerci din nou."
            );

            redirectAttributes.addFlashAttribute(
                    "raspunsuriFormular",
                    extrageRaspunsuriFormular(raspunsuri)
            );

            return "redirect:/chestionare/completare/"
                    + raspunsChestionarId;
        }

        // Serviciul a revenit după confirmarea tranzacției.
        // Auditul nu trebuie să prezinte o salvare încheiată ca eșuată.
        try {
            auditService.inregistreaza(
                    auditService.utilizatorCurent(session),
                    AuditLog.ActiuneAudit.CREARE,
                    AuditLog.TipResursa.CHESTIONAR,
                    raspunsChestionarId,
                    rezultat.pacientId()
            );
        } catch (Exception e) {
            log.error(
                    "Chestionarul {} a fost salvat, "
                            + "dar înregistrarea auditului a eșuat",
                    raspunsChestionarId,
                    e
            );
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Chestionarul '" + rezultat.numeChestionar()
                        + "' a fost completat cu succes!"
        );

        return "redirect:/chestionare/pacient/"
                + rezultat.pacientUserId()
                + "/disponibile";
    }

    @Transactional
    @PostMapping("/sterge/{raspunsIntrebareId}")
    @ResponseBody
    public Map<String, String> stergeRaspuns(
            @PathVariable UUID raspunsIntrebareId,
            HttpSession session) {

        accesChestionarService.verificaStergereRaspuns(
                session,
                raspunsIntrebareId
        );

        raspunsuriIntrebariService.stergeRaspuns(
                raspunsIntrebareId
        );

        return Map.of(
                "status", "success",
                "message", "Răspuns șters cu succes."
        );
    }

    private Map<String, List<String>> extrageRaspunsuriFormular(
            MultiValueMap<String, String> raspunsuri) {

        Map<String, List<String>> formular = new LinkedHashMap<>();

        raspunsuri.forEach((cheie, valori) -> {
            if (cheie.startsWith("intrebare_")) {
                formular.put(cheie, List.copyOf(valori));
            }
        });

        return formular;
    }
}