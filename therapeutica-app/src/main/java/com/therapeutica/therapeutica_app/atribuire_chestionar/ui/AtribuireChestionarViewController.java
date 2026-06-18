package com.therapeutica.therapeutica_app.atribuire_chestionar.ui;

import com.therapeutica.therapeutica_app.atribuire_chestionar.AtribuireChestionarRequestDTO;
import com.therapeutica.therapeutica_app.atribuire_chestionar.AtribuireChestionarService;
import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.ChestionareRepository;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/medic")
public class AtribuireChestionarViewController {

    private final AtribuireChestionarService atribuireChestionarService;
    private final MediciRepository mediciRepository;
    private final PacientiRepository pacientiRepository;
    private final ChestionareRepository chestionareRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;

    public AtribuireChestionarViewController(AtribuireChestionarService atribuireChestionarService,
                                             MediciRepository mediciRepository,
                                             PacientiRepository pacientiRepository,
                                             ChestionareRepository chestionareRepository,
                                             UtilizatoriRepository utilizatoriRepository,
                                             RaspunsuriChestionareRepository raspunsuriChestionareRepository) {
        this.atribuireChestionarService = atribuireChestionarService;
        this.mediciRepository = mediciRepository;
        this.pacientiRepository = pacientiRepository;
        this.chestionareRepository = chestionareRepository;
        this.utilizatoriRepository = utilizatoriRepository;
        this.raspunsuriChestionareRepository = raspunsuriChestionareRepository;
    }

    /**
     * PAS 1-2: Medic accesează secțiunea "Pacienți"
     */
    @GetMapping("/atribuire-chestionar/pacienti")
    public String listaPacienti(HttpSession session, Model model) {
        System.out.println("ATRIBUIRE CHESTIONAR - listaPacienti ===");

        try {
            // Obține informațiile din sesiune
            SessionInfo sessionInfo = getSessionInfo(session);
            if (!"MEDIC".equals(sessionInfo.userRole())) {
                return "redirect:/login?error=unauthorized";
            }

            // Obține medicul
            MedicInfo medicInfo = getMedicInfo(sessionInfo.userId());

            // Obține pacienții medicului
            List<Pacienti> pacienti = pacientiRepository.findPacientiByMedicIdWithUser(medicInfo.medic().getUserId());

            System.out.println("Found " + pacienti.size() + " patients");

            // Adaugă atributele în model
            model.addAttribute("pacienti", pacienti);
            model.addAttribute("medic", medicInfo.medic());
            model.addAttribute("medicNume", medicInfo.medicUser().getNume());
            model.addAttribute("medicPrenume", medicInfo.medicUser().getPrenume());
            model.addAttribute("userId", sessionInfo.userId().toString());

            return "medic/pacienti-list";

        } catch (Exception e) {
            System.err.println("Error in listaPacienti: " + e.getMessage());
            return handleException(e);
        }
    }

    /**
     * PAS 3-4: Medic selectează un pacient
     */
    @GetMapping("/pacienti/{pacientId}")
    @Transactional
    public String detaliiPacient(@PathVariable UUID pacientId, HttpSession session, Model model) {
        try {
            SessionInfo sessionInfo = getSessionInfo(session);
            MedicInfo medicInfo = getMedicInfo(sessionInfo.userId());

            Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found"));

            // Luăm tot istoricul
            List<RaspunsuriChestionare> totIstoricul = atribuireChestionarService
                    .getIstoricChestionarePacient(pacientId);

            // Împărțim lista în două pentru cele două tabele din HTML
            List<RaspunsuriChestionare> completate = totIstoricul.stream()
                    .filter(rc -> rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.COMPLETAT)
                    .toList();

            List<RaspunsuriChestionare> necompletate = totIstoricul.stream()
                    .filter(rc -> rc.getStatus() == RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT)
                    .toList();

            // Adăugăm atributele de bază
            addCommonAttributesToModel(pacient, medicInfo.medic(), medicInfo.medicUser(),
                    sessionInfo.userId().toString(), model);

            // Satisface cerințele specifice din pacienti-detalii.html
            model.addAttribute("medicId", sessionInfo.userId());
            model.addAttribute("totalChestionareCompletate", (long) completate.size());
            model.addAttribute("totalChestionareNecompletate", (long) necompletate.size());
            model.addAttribute("chestionareCompletate", completate);
            model.addAttribute("chestionareNecompletate", necompletate);

            // Calculăm ultima activitate (cea mai recentă dată de completare)
            Optional<java.time.LocalDateTime> ultima = completate.stream()
                    .map(RaspunsuriChestionare::getCompletatLa)
                    .filter(Objects::nonNull)
                    .max(java.time.LocalDateTime::compareTo);
            model.addAttribute("ultimaActivitate", ultima.orElse(null));

            return "medic/pacienti-detalii";
        } catch (Exception e) {
            e.printStackTrace(); // Loghează eroarea în consolă să o vedem clar
            return handleException(e);
        }
    }

    /**
     * PAS 5-6: Medic apasă "Atribuie chestionar nou"
     */
    @GetMapping("/pacienti/{pacientId}/atribuire-nou")
    public String atribuireChestionarNou(@PathVariable UUID pacientId,
                                         HttpSession session,
                                         Model model) {
        System.out.println("=== ATRIBUIRE CHESTIONAR - atribuireChestionarNou ===");

        try {
            // Obține informațiile din sesiune și medicul
            SessionInfo sessionInfo = getSessionInfo(session);
            MedicInfo medicInfo = getMedicInfo(sessionInfo.userId());

            // Încarcă pacientul cu user
            Pacienti pacient = loadPacientWithUser(pacientId, medicInfo.medic().getUserId());

            // Obține chestionarele disponibile CU DEBUGGING
            List<Chestionare> chestionareDisponibile = atribuireChestionarService
                    .getChestionareDisponibilePentruPacient(pacientId);

            System.out.println("Chestionare disponibile object: " + chestionareDisponibile);
            System.out.println("Size: " + (chestionareDisponibile != null ? chestionareDisponibile.size() : "null"));

            // Adaugă atributele comune
            addCommonAttributesToModel(pacient, medicInfo.medic(), medicInfo.medicUser(),
                    sessionInfo.userId().toString(), model);

            model.addAttribute("chestionareDisponibile",
                    chestionareDisponibile != null ? chestionareDisponibile : new ArrayList<>());

            // Creează DTO-ul
            AtribuireChestionarRequestDTO requestDTO = new AtribuireChestionarRequestDTO();
            requestDTO.setPacientId(pacientId);
            requestDTO.setMedicId(medicInfo.medic().getUserId());
            //requestDTO.setMesajPersonalizat("");
            model.addAttribute("requestDTO", requestDTO);

            // DEBUG final
            System.out.println(" Final model attributes:");
            model.asMap().forEach((key, value) -> {
                if (key.equals("chestionareDisponibile")) {
                    List<Chestionare> list = (List<Chestionare>) value;
                    System.out.println("   " + key + " = List with " + list.size() + " items");
                } else {
                    System.out.println("   " + key + " = " + value);
                }
            });

            return "medic/chestionare/atribuire-chestionar-form";

        } catch (Exception e) {
            System.err.println("Error in atribuireChestionarNou: " + e.getMessage());
            e.printStackTrace();
            return handleException(e);
        }
    }

    //PAS 7-12: Procesează atribuirea

    @PostMapping("/pacienti/{pacientId}/atribuire")
    @Transactional
    public String proceseazaAtribuire(@PathVariable UUID pacientId,
                                      @ModelAttribute @Valid AtribuireChestionarRequestDTO requestDTO,
                                      BindingResult bindingResult,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {

        System.out.println("=== ATRIBUIRE CHESTIONAR - proceseazaAtribuire ===");

        try {
            SessionInfo sessionInfo = getSessionInfo(session);

            // ====== CRITIC: Obține pacientul și verifică ID-ul corect ======
            System.out.println("Loading patient from database...");
            Pacienti pacient = pacientiRepository.findById(pacientId)
                    .orElseThrow(() -> new NotFoundException("Pacient not found"));

            System.out.println("Patient loaded:");
            System.out.println("   - Patient entity ID: " + pacient.getId());
            System.out.println("   - Patient user ID: " +
                    (pacient.getUser() != null ? pacient.getUser().getId() : "null"));


            requestDTO.setPacientId(pacient.getId());
            requestDTO.setMedicId(sessionInfo.userId());

            // Validare
            if (bindingResult.hasErrors() ||
                    requestDTO.getChestionareIds() == null ||
                    requestDTO.getChestionareIds().isEmpty()) {

                redirectAttributes.addFlashAttribute("error",
                        "Selectați cel puțin un chestionar!");
                return "redirect:/medic/pacienti/" + pacientId + "/atribuire-nou";
            }

            // ====== Procesează atribuirea ======

            List<UUID> raspunsuriIds = atribuireChestionarService
                    .atribuiChestionarePacientului(requestDTO);

            System.out.println("Service returned " + raspunsuriIds.size() + " IDs");

            // Verifică imediat în baza de date
            if (!raspunsuriIds.isEmpty()) {
                for (UUID id : raspunsuriIds) {
                    boolean exists = raspunsuriChestionareRepository.existsById(id);
                    System.out.println("   - ID " + id + " exists in DB: " + exists);

                    if (exists) {
                        Optional<RaspunsuriChestionare> rc = raspunsuriChestionareRepository.findById(id);
                        rc.ifPresent(r -> System.out.println("     - Patient ID: " +
                                (r.getPacient() != null ? r.getPacient().getId() : "null")));
                    }
                }
            }

            // Obține numele pacientului pentru mesaj (acum suntem în tranzacție)
            Utilizatori pacientUser = pacient.getUser();
            String numePacient = (pacientUser != null) ?
                    pacientUser.getNume() + " " + pacientUser.getPrenume() : "Pacient";

            // Mesaj de succes
            redirectAttributes.addFlashAttribute("success",
                    "Chestionarul a fost atribuit cu succes pacientului " + numePacient + "!");
            redirectAttributes.addFlashAttribute("numarAtribuit", raspunsuriIds.size());

            System.out.println("Redirecting to patient details page...");
            return "redirect:/medic/pacienti/" + pacientId;

        } catch (IllegalStateException e) {
            // Prindem excepția de validare din Service pentru duplicate active
            System.err.println("Validation Error in proceseazaAtribuire: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // Redirecționăm înapoi la formularul de atribuire, nu la profilul pacientului
            return "redirect:/medic/pacienti/" + pacientId + "/atribuire-nou";

        } catch (Exception e) {
            System.err.println("Error in proceseazaAtribuire: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Eroare la atribuirea chestionarului");
            return "redirect:/medic/pacienti/" + pacientId;
        }
    }

    /**
     * Medicul a ales un chestionar și acum trebuie să aleagă pacientul.
     */
    @GetMapping("/atribuire-chestionar/selecteaza-pacient/{chestionarId}")
    public String selecteazaPacientPentruChestionar(@PathVariable UUID chestionarId,
                                                    HttpSession session,
                                                    Model model) {
        // 1. Validăm că există chestionarul
        Chestionare chestionar = chestionareRepository.findById(chestionarId)
                .orElseThrow(() -> new NotFoundException("Chestionar negăsit"));

        // 2. Luăm pacienții (refolosim logica ta existentă din listaPacienti)
        SessionInfo sessionInfo = getSessionInfo(session);
        MedicInfo medicInfo = getMedicInfo(sessionInfo.userId());
        List<Pacienti> pacienti = pacientiRepository.findPacientiByMedicIdWithUser(medicInfo.medic().getUserId());

        // 3. Trimitem datele către o pagină de selecție
        model.addAttribute("chestionar", chestionar);
        model.addAttribute("pacienti", pacienti);
        model.addAttribute("medicUser", medicInfo.medicUser());

        // Putem refolosi un template existent sau unul simplificat
        return "medic/chestionare/selectie-pacient-atribuire";
    }



    // METODE HELPER

    // Record pentru informații sesiune
    private record SessionInfo(UUID userId, String userRole) {}

    // Record pentru informații medic
    private record MedicInfo(Utilizatori medicUser, Medici medic) {}

    private SessionInfo getSessionInfo(HttpSession session) {
        String userIdStr = getUserIdStringFromSession(session);
        String userRole = getUserRoleFromSession(session);
        UUID userId = UUID.fromString(userIdStr);

        return new SessionInfo(userId, userRole);
    }

    private MedicInfo getMedicInfo(UUID userId) {
        Utilizatori medicUser = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Medici medic = mediciRepository.findById(medicUser.getId())
                .orElseThrow(() -> new NotFoundException("Medic not found"));

        return new MedicInfo(medicUser, medic);
    }

    private Pacienti loadPacientWithUser(UUID pacientId, UUID medicId) {
        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                .orElseThrow(() -> new NotFoundException("Pacient not found"));

        // Verifică dacă pacientul aparține medicului
        if (!pacient.getMedic().getUserId().equals(medicId)) {
            throw new SecurityException("Pacientul nu aparține medicului");
        }

        return pacient;
    }

    private void addCommonAttributesToModel(Pacienti pacient, Medici medic,
                                            Utilizatori medicUser, String userIdStr,
                                            Model model) {
        Utilizatori pacientUser = pacient.getUser();

        // Debugging
        if (pacientUser == null) {
            System.err.println("⚠ ATENȚIE: Pacientul cu ID " + pacient.getId() + " nu are un Utilizator asociat!");
        }

        model.addAttribute("pacient", pacient);
        model.addAttribute("pacientUser", pacientUser);
        model.addAttribute("medic", medic);
        model.addAttribute("medicUser", medicUser);
        model.addAttribute("userId", userIdStr);
    }

    private String getUserIdStringFromSession(HttpSession session) {
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("User not authenticated - Session expired");
        }
        return userIdObj.toString();
    }

    private String getUserRoleFromSession(HttpSession session) {
        Object userRoleObj = session.getAttribute("userRole");
        if (userRoleObj == null) {
            throw new RuntimeException("User role not found in session");
        }
        return userRoleObj.toString();
    }

    private String handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            System.err.println("❌ Invalid UUID format: " + e.getMessage());
            return "redirect:/login?error=invalid_session";
        } else if (e instanceof SecurityException) {
            System.err.println("❌ Security error: " + e.getMessage());
            return "redirect:/medic/atribuire-chestionar/pacienti?error=acces_nepermis";
        } else {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login?error=session_expired";
        }
    }
}