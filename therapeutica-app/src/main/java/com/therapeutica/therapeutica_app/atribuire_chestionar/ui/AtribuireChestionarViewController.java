package com.therapeutica.therapeutica_app.atribuire_chestionar.ui;

import com.therapeutica.therapeutica_app.atribuire_chestionar.AtribuireChestionarRequestDTO;
import com.therapeutica.therapeutica_app.atribuire_chestionar.AtribuireChestionarService;
import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.ChestionareRepository;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/medic")
@RequiredArgsConstructor
@Slf4j
public class AtribuireChestionarViewController {

    private final AtribuireChestionarService atribuireChestionarService;
    private final MediciRepository mediciRepository;
    private final PacientiRepository pacientiRepository;
    private final ChestionareRepository chestionareRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final AuditService auditService;

    /**
     * Lista pacienților medicului, punct de plecare pentru atribuirea de chestionare.
     */
    @GetMapping("/atribuire-chestionar/pacienti")
    public String listaPacienti(HttpSession session, Model model) {

        try {
            SessionInfo sessionInfo = getSessionInfo(session);

            if (!"MEDIC".equals(sessionInfo.userRole())) {
                log.warn("Acces refuzat la lista de pacienți pentru rolul: {}", sessionInfo.userRole());
                return "redirect:/login?error=unauthorized";
            }

            MedicInfo medicInfo = getMedicInfo(sessionInfo.userId());

            List<Pacienti> pacienti = pacientiRepository
                    .findPacientiByMedicIdWithUser(medicInfo.medic().getUserId());

            log.info("Lista pacienților pentru atribuire: {} pacienți", pacienti.size());

            model.addAttribute("pacienti", pacienti);
            model.addAttribute("medic", medicInfo.medic());
            model.addAttribute("medicNume", medicInfo.medicUser().getNume());
            model.addAttribute("medicPrenume", medicInfo.medicUser().getPrenume());
            model.addAttribute("userId", sessionInfo.userId().toString());

            return "medic/pacienti-list";

        } catch (Exception e) {
            log.error("Eroare la încărcarea listei de pacienți: {}", e.getMessage());
            return handleException(e);
        }
    }

    /**
     * Formularul de atribuire: medicul alege chestionarele pentru un pacient.
     */
    @GetMapping("/pacienti/{pacientId}/atribuire-nou")
    public String atribuireChestionarNou(@PathVariable UUID pacientId,
                                         HttpSession session,
                                         Model model) {

        try {
            SessionInfo sessionInfo = getSessionInfo(session);
            MedicInfo medicInfo = getMedicInfo(sessionInfo.userId());

            Pacienti pacient = loadPacientWithUser(pacientId, medicInfo.medic().getUserId());

            List<Chestionare> chestionareDisponibile = atribuireChestionarService
                    .getChestionareDisponibilePentruPacient(pacientId);

            if (chestionareDisponibile == null) {
                chestionareDisponibile = new ArrayList<>();
            }

            log.info("Formular atribuire pentru pacientul {}: {} chestionare disponibile",
                    pacientId, chestionareDisponibile.size());

            addCommonAttributesToModel(pacient, medicInfo.medic(), medicInfo.medicUser(),
                    sessionInfo.userId().toString(), model);

            model.addAttribute("chestionareDisponibile", chestionareDisponibile);

            AtribuireChestionarRequestDTO requestDTO = new AtribuireChestionarRequestDTO();
            requestDTO.setPacientId(pacientId);
            requestDTO.setMedicId(medicInfo.medic().getUserId());
            model.addAttribute("requestDTO", requestDTO);

            return "medic/chestionare/atribuire-chestionar-form";

        } catch (Exception e) {
            log.error("Eroare la afișarea formularului de atribuire pentru pacientul {}: {}",
                    pacientId, e.getMessage());
            return handleException(e);
        }
    }

    /**
     * Procesează atribuirea chestionarelor selectate către pacient.
     */
    @PostMapping("/pacienti/{pacientId}/atribuire")
    @Transactional
    public String proceseazaAtribuire(@PathVariable UUID pacientId,
                                      @ModelAttribute @Valid AtribuireChestionarRequestDTO requestDTO,
                                      BindingResult bindingResult,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {

        SessionInfo sessionInfo;

        try {
            sessionInfo = getSessionInfo(session);
        } catch (Exception e) {
            log.error("Sesiune invalidă la atribuirea chestionarului: {}", e.getMessage());
            return handleException(e);
        }

        try {
            Pacienti pacient = pacientiRepository.findById(pacientId)
                    .orElseThrow(() -> new NotFoundException("Pacientul nu a fost găsit"));

            requestDTO.setPacientId(pacient.getId());
            requestDTO.setMedicId(sessionInfo.userId());

            boolean selectieInvalida = bindingResult.hasErrors()
                    || requestDTO.getChestionareIds() == null
                    || requestDTO.getChestionareIds().isEmpty();

            if (selectieInvalida) {
                redirectAttributes.addFlashAttribute("error", "Selectați cel puțin un chestionar!");
                return "redirect:/medic/pacienti/" + pacientId + "/atribuire-nou";
            }

            List<UUID> raspunsuriIds = atribuireChestionarService
                    .atribuiChestionarePacientului(requestDTO);

            for (UUID raspunsId : raspunsuriIds) {
                auditService.inregistreaza(
                        sessionInfo.userId(),
                        AuditLog.ActiuneAudit.CREARE,
                        AuditLog.TipResursa.CHESTIONAR,
                        raspunsId,
                        pacient.getId()
                );
            }

            log.info("Atribuite {} chestionare pacientului {}", raspunsuriIds.size(), pacientId);

            Utilizatori pacientUser = pacient.getUser();
            String numePacient = (pacientUser != null)
                    ? pacientUser.getNume() + " " + pacientUser.getPrenume()
                    : "pacient";

            redirectAttributes.addFlashAttribute("success",
                    "Chestionarul a fost atribuit cu succes pacientului " + numePacient + "!");
            redirectAttributes.addFlashAttribute("numarAtribuit", raspunsuriIds.size());

            return "redirect:/medic/" + sessionInfo.userId() + "/pacient/" + pacientId;

        } catch (IllegalStateException e) {
            // Serviciul semnalează o atribuire duplicată activă
            log.warn("Atribuire respinsă pentru pacientul {}: {}", pacientId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/medic/pacienti/" + pacientId + "/atribuire-nou";

        } catch (Exception e) {
            log.error("Eroare la atribuirea chestionarului pentru pacientul {}: {}",
                    pacientId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Eroare la atribuirea chestionarului");
            return "redirect:/medic/" + sessionInfo.userId() + "/pacient/" + pacientId;
        }
    }

    /**
     * Fluxul invers: medicul a ales întâi chestionarul și alege apoi pacientul.
     */
    @GetMapping("/atribuire-chestionar/selecteaza-pacient/{chestionarId}")
    public String selecteazaPacientPentruChestionar(@PathVariable UUID chestionarId,
                                                    HttpSession session,
                                                    Model model) {
        try {
            Chestionare chestionar = chestionareRepository.findById(chestionarId)
                    .orElseThrow(() -> new NotFoundException("Chestionar negăsit"));

            SessionInfo sessionInfo = getSessionInfo(session);
            MedicInfo medicInfo = getMedicInfo(sessionInfo.userId());

            List<Pacienti> pacienti = pacientiRepository
                    .findPacientiByMedicIdWithUser(medicInfo.medic().getUserId());

            model.addAttribute("chestionar", chestionar);
            model.addAttribute("pacienti", pacienti);
            model.addAttribute("medicUser", medicInfo.medicUser());

            return "medic/chestionare/selectie-pacient-atribuire";

        } catch (Exception e) {
            log.error("Eroare la selecția pacientului pentru chestionarul {}: {}",
                    chestionarId, e.getMessage());
            return handleException(e);
        }
    }

    // ========== METODE HELPER ==========

    private record SessionInfo(UUID userId, String userRole) {}

    private record MedicInfo(Utilizatori medicUser, Medici medic) {}

    private SessionInfo getSessionInfo(HttpSession session) {
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) {
            throw new IllegalStateException("Sesiune expirată sau utilizator neautentificat");
        }

        Object userRoleObj = session.getAttribute("userRole");
        if (userRoleObj == null) {
            throw new IllegalStateException("Rolul utilizatorului lipsește din sesiune");
        }

        return new SessionInfo(UUID.fromString(userIdObj.toString()), userRoleObj.toString());
    }

    private MedicInfo getMedicInfo(UUID userId) {
        Utilizatori medicUser = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilizatorul nu a fost găsit"));

        Medici medic = mediciRepository.findById(medicUser.getId())
                .orElseThrow(() -> new NotFoundException("Medicul nu a fost găsit"));

        return new MedicInfo(medicUser, medic);
    }

    /**
     * Încarcă pacientul și verifică apartenența la medicul curent.
     */
    private Pacienti loadPacientWithUser(UUID pacientId, UUID medicId) {
        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                .orElseThrow(() -> new NotFoundException("Pacientul nu a fost găsit"));

        if (pacient.getMedic() == null || !pacient.getMedic().getUserId().equals(medicId)) {
            throw new SecurityException("Pacientul nu aparține medicului curent");
        }

        return pacient;
    }

    private void addCommonAttributesToModel(Pacienti pacient, Medici medic,
                                            Utilizatori medicUser, String userIdStr,
                                            Model model) {
        model.addAttribute("pacient", pacient);
        model.addAttribute("pacientUser", pacient.getUser());
        model.addAttribute("medic", medic);
        model.addAttribute("medicUser", medicUser);
        model.addAttribute("userId", userIdStr);
    }

    private String handleException(Exception e) {
        if (e instanceof SecurityException) {
            log.warn("Acces nepermis: {}", e.getMessage());
            return "redirect:/medic/atribuire-chestionar/pacienti?error=acces_nepermis";
        }

        if (e instanceof IllegalArgumentException) {
            log.warn("Identificator invalid în sesiune: {}", e.getMessage());
            return "redirect:/login?error=invalid_session";
        }

        log.error("Eroare neașteptată: {}", e.getMessage(), e);
        return "redirect:/login?error=session_expired";
    }
}