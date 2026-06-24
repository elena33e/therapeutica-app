package com.therapeutica.therapeutica_app.cod_inregistrare.ui;

import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrareService;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.CodInregistrareDTO;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodRequest;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/generare-cod")
public class GenerareCodController {

    private final CodInregistrareService codInregistrareService;

    public GenerareCodController(CodInregistrareService codInregistrareService) {
        this.codInregistrareService = codInregistrareService;
    }

    @GetMapping
    public String afiseazaPagina(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) {
            return "redirect:/login?redirect=/generare-cod";
        }
        if (!"MEDIC".equals(userRole) && !"ADMINISTRATOR".equals(userRole)) {
            return "redirect:/dashboard?error=access-denied";
        }

        adaugaStatisticiInModel(UUID.fromString(userId), model);
        model.addAttribute("request", new GenerareCodRequest());
        return "generare-cod";
    }

    @PostMapping
    public String generareCod(@ModelAttribute GenerareCodRequest request,
                              HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) {
            return "redirect:/login?redirect=/generare-cod";
        }
        if (!"MEDIC".equals(userRole) && !"ADMINISTRATOR".equals(userRole)) {
            return "redirect:/dashboard?error=access-denied";
        }

        request.setMedicId(UUID.fromString(userId));
        GenerareCodResponse response = codInregistrareService.generareCodInregistrare(request);
        model.addAttribute("rezultat", response);

        adaugaStatisticiInModel(UUID.fromString(userId), model);
        model.addAttribute("request", new GenerareCodRequest());
        return "generare-cod";
    }

    private void adaugaStatisticiInModel(UUID medicId, Model model) {
        List<CodInregistrareDTO> coduri = codInregistrareService.getCoduriByMedic(medicId);
        long coduriActive = coduri.stream()
                .filter(c -> c.getStatus() == CodInregistrare.StatusCod.NEUTILIZAT)
                .count();

        model.addAttribute("coduri", coduri);
        model.addAttribute("totalCoduri", coduri.size());
        model.addAttribute("coduriActive", coduriActive);
        model.addAttribute("coduriUtilizate", coduri.size() - coduriActive);
    }
}