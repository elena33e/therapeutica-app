package com.therapeutica.therapeutica_app.auth.ui;
import com.therapeutica.therapeutica_app.auth.InregistrareService;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareRequest;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class InregistrareController {
    @Autowired
    private InregistrareService inregistrareService;

    @GetMapping("/inregistrare")
    public String afiseazaPaginaInregistrare() {
        return "auth/inregistrare"; // Randează șablonul inregistrare.html
    }

    @PostMapping("/inregistrare")
    public String proceseazaInregistrarea(@ModelAttribute InregistrareRequest request, Model model) {
        try {
            InregistrareResponse response = inregistrareService.inregistreazaUtilizator(request);

            if (response.isSuccess()) {
                return "redirect:/login?inregistrare=succes";
            } else {
                model.addAttribute("eroare", response.getMessage());
                return "auth/inregistrare";
            }
        } catch (Exception e) {
            model.addAttribute("eroare", "Eroare internă de server.");
            return "auth/inregistrare";
        }
    }
}
