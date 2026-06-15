package com.therapeutica.therapeutica_app.pacienti.ui;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import com.therapeutica.therapeutica_app.notificari.external.WhatsAppService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/pacient/profil")
public class PacientProfilController {

    @Autowired
    private UtilizatoriRepository utilizatoriRepository;

    @Autowired
    private PacientiRepository pacientiRepository;

    @Autowired
    private WhatsAppService whatsappService;

    // Vizualizare profil (F5)
    @GetMapping("/{id}")
    public String vizualizareProfil(@PathVariable String id,
                                    HttpSession session,
                                    Model model) {

        // Verifică autentificare și rol
        String userIdStr = (String) session.getAttribute("userId");


        UUID userId = UUID.fromString(userIdStr);

        // Verifică dacă ID-ul din URL corespunde cu utilizatorul autentificat
        if (!id.equals(userIdStr)) {
            return "redirect:/pacient/profil/" + userIdStr;
        }

        // Obține utilizatorul și pacientul
        Utilizatori utilizator = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit"));

        Pacienti pacient = pacientiRepository.findByUserIdWithFullData(userId)
                .orElseGet(() -> {
                    Pacienti p = new Pacienti();
                    p.setUser(utilizator); // Leagă pacientul nou de utilizatorul curent
                    return p;
                });

        // --- LOGICA WHATSAPP ---
        // Verificăm dacă pacientul are medic și dacă medicul are un telefon înregistrat
        if (pacient.getMedic() != null && pacient.getMedic().getTelefon() != null) {
            String numeMedic = pacient.getMedic().getUser() != null ? pacient.getMedic().getUser().getNume() : "";

            // Construim un mesaj politicos de start
            String mesajStart = "Bună ziua, Dr. " + numeMedic + ". Vă contactez din portalul Therapeutica. " +
                    "Sunt pacientul dumneavoastră, " + utilizator.getNume() + " " + utilizator.getPrenume() + ".";

            // Generăm link-ul prin serviciu
            String linkWa = whatsappService.genereazaLinkContactPersonalizat(pacient.getMedic().getTelefon(), mesajStart);

            // Punem link-ul în model pentru Thymeleaf
            model.addAttribute("whatsappLinkMedic", linkWa);
        }


        // Adaugă datele în model
        model.addAttribute("pacientId", userIdStr);
        model.addAttribute("utilizator", utilizator);
        model.addAttribute("pacient", pacient);
        model.addAttribute("pageTitle", "Profil Pacient");
        model.addAttribute("mode", "view");

        return "pacient/profil";
    }

    // Afișare formular editare (F6 - pasul 1-2)
    @GetMapping("/{id}/editare")
    public String formularEditareProfil(@PathVariable String id,
                                        HttpSession session,
                                        Model model) {

        String userIdStr = (String) session.getAttribute("userId");

        UUID userId = UUID.fromString(userIdStr);

        if (!id.equals(userIdStr)) {
            return "redirect:/pacient/profil/" + userIdStr + "/editare";
        }

        Utilizatori utilizator = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit"));

        Optional<Pacienti> pacientOpt = pacientiRepository.findByUserId(userId);

        model.addAttribute("pacientId", userIdStr);
        model.addAttribute("utilizator", utilizator);
        model.addAttribute("pacient", pacientOpt.orElse(new Pacienti()));
        model.addAttribute("pageTitle", "Editare Profil");
        model.addAttribute("mode", "edit");

        return "pacient/profil";
    }

    // Salvare modificări (F6 - pasul 3-7)
    @PostMapping("/{id}/salvare")
    public String salvareProfil(@PathVariable String id,
                                @RequestParam String nume,
                                @RequestParam String prenume,
                                @RequestParam String email,
                                @RequestParam(required = false) String cnp,
                                @RequestParam(required = false) String dataNasterii,
                                @RequestParam(required = false) String sex,
                                @RequestParam(required = false) Integer inaltime,
                                @RequestParam(required = false) Double greutate,
                                @RequestParam(required = false) String grupaSangvina,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        String userIdStr = (String) session.getAttribute("userId");

        UUID userId = UUID.fromString(userIdStr);
        if (!id.equals(userIdStr)) {
            return "redirect:/pacient/profil/" + userIdStr + "/editare";
        }

        // 2. Obținere entități
        Utilizatori utilizator = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit"));

        Pacienti pacient = pacientiRepository.findByUserId(userId)
                .orElse(new Pacienti());

        // 3. Validări fundamentale
        boolean hasErrors = false;

        if (nume == null || nume.trim().isEmpty()) {
            model.addAttribute("numeError", "Numele este obligatoriu");
            hasErrors = true;
        }
        if (prenume == null || prenume.trim().isEmpty()) {
            model.addAttribute("prenumeError", "Prenumele este obligatoriu");
            hasErrors = true;
        }
        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("emailError", "Email-ul este obligatoriu");
            hasErrors = true;
        } else {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!email.matches(emailRegex)) {
                model.addAttribute("emailError", "Format email invalid");
                hasErrors = true;
            } else {
                utilizatoriRepository.findByEmail(email).ifPresent(u -> {
                    if (!u.getId().equals(userId)) {
                        model.addAttribute("emailError", "Acest email este deja înregistrat");
                    }
                });
                if (model.containsAttribute("emailError")) hasErrors = true;
            }
        }

        // 4. Tratare erori (Return la formular)
        if (hasErrors) {
            model.addAttribute("pacientId", userIdStr);
            model.addAttribute("utilizator", utilizator);
            model.addAttribute("pacient", pacient);
            model.addAttribute("pageTitle", "Editare Profil");
            model.addAttribute("mode", "edit");
            // Păstrăm valorile introduse de user în caz de eroare
            utilizator.setNume(nume);
            utilizator.setPrenume(prenume);
            utilizator.setEmail(email);
            return "pacient/profil";
        }

        // 5. Actualizare date Utilizatori
        utilizator.setNume(nume.trim());
        utilizator.setPrenume(prenume.trim());
        utilizator.setEmail(email.trim());

        // 6. Actualizare date Pacienti (Biometrie)
        pacient.setCnp(cnp != null && !cnp.isEmpty() ? cnp.trim() : null);
        pacient.setInaltime(inaltime);
        pacient.setGreutate(greutate);
        pacient.setGrupaSangvina(grupaSangvina);

        // Conversie Data Nașterii
        if (dataNasterii != null && !dataNasterii.isEmpty()) {
            try {
                pacient.setDataNasterii(java.time.LocalDate.parse(dataNasterii));
            } catch (java.time.format.DateTimeParseException e) {
                // Log error sau ignoră dacă formatul e invalid din browser
            }
        }

        // Conversie Enum Sex
        if (sex != null && !sex.isEmpty()) {
            try {
                pacient.setSex(com.therapeutica.therapeutica_app.pacienti.Sex.valueOf(sex));
            } catch (IllegalArgumentException e) {
                pacient.setSex(com.therapeutica.therapeutica_app.pacienti.Sex.NEPRECIZAT);
            }
        }

        // Legătura OneToOne
        if (pacient.getUser() == null) {
            pacient.setUser(utilizator);
        }

        // 7. Persistență
        utilizatoriRepository.save(utilizator);
        pacientiRepository.save(pacient);

        // 8. Sync Sesiune
        session.setAttribute("userNume", utilizator.getNume());
        session.setAttribute("userPrenume", utilizator.getPrenume());
        session.setAttribute("userEmail", utilizator.getEmail());

        redirectAttributes.addFlashAttribute("successMessage", "Profilul medical a fost actualizat cu succes!");

        return "redirect:/pacient/profil/" + userIdStr;
    }
}