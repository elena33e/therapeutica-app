package com.therapeutica.therapeutica_app.medici.ui;

import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrareRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class DashboardMedicController {

    private final UtilizatoriRepository utilizatoriRepository;
    private final MediciRepository mediciRepository;
    private final PacientiRepository pacientiRepository;
    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final CodInregistrareRepository codInregistrareRepository;

    public DashboardMedicController(UtilizatoriRepository utilizatoriRepository,
                                    MediciRepository mediciRepository,
                                    PacientiRepository pacientiRepository,
                                    RaspunsuriChestionareRepository raspunsuriChestionareRepository,
                                    CodInregistrareRepository codInregistrareRepository) {
        this.utilizatoriRepository = utilizatoriRepository;
        this.mediciRepository = mediciRepository;
        this.pacientiRepository = pacientiRepository;
        this.raspunsuriChestionareRepository = raspunsuriChestionareRepository;
        this.codInregistrareRepository = codInregistrareRepository;
    }

    @GetMapping("/medic/dashboard/{medicId}")
    public String dashboardMedic(@PathVariable String medicId,
                                 HttpSession session,
                                 Model model) {

        String sessionUserId = (String) session.getAttribute("userId");
        String sessionUserRole = (String) session.getAttribute("userRole");

        System.out.println("=== MEDIC DASHBOARD ===");
        System.out.println("Requested medicId: " + medicId);
        System.out.println("Session userId: " + sessionUserId);
        System.out.println("Session role: " + sessionUserRole);

        if (sessionUserId == null) {
            return "redirect:/login";
        }

        if (!sessionUserId.equals(medicId)) {
            return "redirect:/medic/dashboard/" + sessionUserId;
        }

        // Verificare rol
        if (!"MEDIC".equals(sessionUserRole) && !"ADMINISTRATOR".equals(sessionUserRole)) {
            if ("PACIENT".equals(sessionUserRole)) {
                return "redirect:/pacient/dashboard/" + sessionUserId;
            }
            return "redirect:/login?error=wrong-role";
        }

        try {
            // Date medic
            UUID medicUuid = UUID.fromString(medicId);
            Utilizatori medicUser = utilizatoriRepository.findById(medicUuid)
                    .orElseThrow(() -> new RuntimeException("Medic not found"));

            // Obține entitatea Medici
            Medici medic = mediciRepository.findById(medicUuid)
                    .orElseThrow(() -> new RuntimeException("Medic entity not found"));

            // Statistici coduri
            long coduriGenerate = codInregistrareRepository.findByGeneratDeId(medicUuid).size();
            long coduriActive = codInregistrareRepository.findByGeneratDeIdAndStatus(medicUuid,
                    com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare.StatusCod.NEUTILIZAT).size();

            // Statistici pacienți
            long numarPacienti = pacientiRepository.findAll().stream()
                    .filter(p -> p.getMedic() != null && p.getMedic().getUserId().equals(medic.getUserId()))
                    .count();


            long numarChestionareAtribuite = raspunsuriChestionareRepository.findAll().stream()
                    .filter(r -> r.getMedic() != null && r.getMedic().getUserId().equals(medic.getUserId()))
                    .count();

            long numarChestionareNecompletate = raspunsuriChestionareRepository.findAll().stream()
                    .filter(r -> r.getMedic() != null &&
                            r.getMedic().getUserId().equals(medic.getUserId()) &&
                            r.getStatus() == com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT)
                    .count();

            long numarChestionareCompletate = raspunsuriChestionareRepository.findAll().stream()
                    .filter(r -> r.getMedic() != null &&
                            r.getMedic().getUserId().equals(medic.getUserId()) &&
                            r.getStatus() == com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare.StatusRaspuns.COMPLETAT)
                    .count();

            // Adaugă atributele în model
            model.addAttribute("medic", medic);
            model.addAttribute("medicUser", medicUser);
            model.addAttribute("userId", sessionUserId);
            model.addAttribute("userRole", sessionUserRole);
            model.addAttribute("userEmail", session.getAttribute("userEmail"));
            model.addAttribute("userNume", medicUser.getNume());
            model.addAttribute("userPrenume", medicUser.getPrenume());
            model.addAttribute("medicNumeComplet", medicUser.getNume() + " " + medicUser.getPrenume());

            // Statistici coduri
            model.addAttribute("coduriGenerate", coduriGenerate);
            model.addAttribute("coduriActive", coduriActive);
            model.addAttribute("coduriUtilizate", coduriGenerate - coduriActive);

            // ✅ NOU: Statistici pacienți și chestionare
            model.addAttribute("numarPacienti", numarPacienti);
            model.addAttribute("numarChestionareAtribuite", numarChestionareAtribuite);
            model.addAttribute("numarChestionareNecompletate", numarChestionareNecompletate);
            model.addAttribute("numarChestionareCompletate", numarChestionareCompletate);

            System.out.println("✅ Medic dashboard loaded for: " + medicUser.getNume() + " " + medicUser.getPrenume());
            System.out.println("📊 Statistici - Pacienți: " + numarPacienti);
            System.out.println("📊 Statistici - Chestionare atribuite: " + numarChestionareAtribuite);

        } catch (Exception e) {
            System.out.println("❌ Error loading medic data: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Nu s-au putut încărca datele medicului");
        }

        return "medic/dashboard";
    }
}