package com.therapeutica.therapeutica_app.medici.ui;

import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/medic/profil")
public class MedicProfilController {

    @Autowired
    private UtilizatoriRepository utilizatoriRepository;

    @Autowired
    private MediciRepository mediciRepository;

    // Vizualizare profil medic
    @GetMapping("/{id}")
    public String vizualizareProfil(@PathVariable String id,
                                    HttpSession session,
                                    Model model) {

        String userIdStr = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userIdStr == null || userRole == null || !userRole.equals("MEDIC")) {
            return "redirect:/login";
        }

        UUID userId = UUID.fromString(userIdStr);

        if (!id.equals(userIdStr)) {
            return "redirect:/medic/profil/" + userIdStr;
        }

        Utilizatori utilizator = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit"));

        Optional<Medici> medicOpt = mediciRepository.findByUserId(userId);

        model.addAttribute("medicId", userIdStr);
        model.addAttribute("utilizator", utilizator);
        model.addAttribute("medic", medicOpt.orElse(null));
        model.addAttribute("pageTitle", "Profil Medic");
        model.addAttribute("mode", "view");

        return "medic/profil";
    }

    // Afișare formular editare
    @GetMapping("/{id}/editare")
    public String formularEditareProfil(@PathVariable String id,
                                        HttpSession session,
                                        Model model) {

        String userIdStr = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userIdStr == null || userRole == null || !userRole.equals("MEDIC")) {
            return "redirect:/login";
        }

        UUID userId = UUID.fromString(userIdStr);

        if (!id.equals(userIdStr)) {
            return "redirect:/medic/profil/" + userIdStr + "/editare";
        }

        Utilizatori utilizator = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit"));

        Optional<Medici> medicOpt = mediciRepository.findByUserId(userId);

        model.addAttribute("medicId", userIdStr);
        model.addAttribute("utilizator", utilizator);
        model.addAttribute("medic", medicOpt.orElse(new Medici()));
        model.addAttribute("pageTitle", "Editare Profil Medic");
        model.addAttribute("mode", "edit");

        return "medic/profil";
    }

    @Transactional // Esențial: ori se salvează ambele, ori niciuna
    @PostMapping("/{id}/salvare")
    public String salvareProfil(@PathVariable String id,
                                @RequestParam String nume,
                                @RequestParam String prenume,
                                @RequestParam String email,
                                @RequestParam(required = false) String specializare,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        String userIdStr = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        // 1. Securitate: Verifică dacă user-ul are dreptul să facă asta
        if (userIdStr == null || !userRole.equals("MEDIC") || !id.equals(userIdStr)) {
            return "redirect:/login";
        }

        UUID userId = UUID.fromString(userIdStr);

        // 2. Obține entitățile existente
        Utilizatori utilizator = utilizatoriRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit"));

        Medici medic = mediciRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Medici m = new Medici();
                    m.setUser(utilizator);
                    return m;
                });

        // 3. Validare manuală
        boolean hasErrors = false;

        if (nume == null || nume.trim().isEmpty()) {
            model.addAttribute("numeError", "Numele este obligatoriu");
            hasErrors = true;
        }
        if (prenume == null || prenume.trim().isEmpty()) {
            model.addAttribute("prenumeError", "Prenumele este obligatoriu");
            hasErrors = true;
        }

        // Validare email
        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("emailError", "Email-ul este obligatoriu");
            hasErrors = true;
        } else {
            Optional<Utilizatori> existent = utilizatoriRepository.findByEmail(email.trim());
            if (existent.isPresent() && !existent.get().getId().equals(userId)) {
                model.addAttribute("emailError", "Acest email este deja utilizat de alt cont");
                hasErrors = true;
            }
        }

        // 4. Tratare Erori: Reafișăm formularul cu datele "murdare" (ce a scris userul)
        if (hasErrors) {
            // Actualizăm obiectele temporar pentru ca Thymeleaf să afișeze ce a scris userul în input-uri
            utilizator.setNume(nume);
            utilizator.setPrenume(prenume);
            utilizator.setEmail(email);
            medic.setSpecializare(specializare);

            model.addAttribute("medicId", userIdStr);
            model.addAttribute("utilizator", utilizator);
            model.addAttribute("medic", medic);
            model.addAttribute("mode", "edit");
            model.addAttribute("pageTitle", "Editare Profil Medic");
            return "medic/profil";
        }

        // 5. Salvare date
        utilizator.setNume(nume.trim());
        utilizator.setPrenume(prenume.trim());
        utilizator.setEmail(email.trim().toLowerCase());
        utilizatoriRepository.save(utilizator);

        medic.setSpecializare(specializare != null ? specializare.trim() : null);
        mediciRepository.save(medic);

        // 6. Actualizare Sesiune (Să se schimbe numele în header imediat)
        session.setAttribute("userNume", utilizator.getNume());
        session.setAttribute("userPrenume", utilizator.getPrenume());

        // 7. Finalizare
        redirectAttributes.addFlashAttribute("successMessage", "Profilul a fost actualizat!");
        return "redirect:/medic/profil/" + userIdStr;
    }
}