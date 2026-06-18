package com.therapeutica.therapeutica_app.resetare_parola.ui;

import com.therapeutica.therapeutica_app.resetare_parola.ResetareParolaService;
import com.therapeutica.therapeutica_app.util.PasswordValidator;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resetare-parola")
@RequiredArgsConstructor
public class ResetareParolaController {

    private final ResetareParolaService resetareParolaService;
    private final UtilizatoriRepository utilizatoriRepository;

    // Afișează pagina unde utilizatorul introduce email-ul
    @GetMapping("/cere-resetare-view")
    public String cereResetareView() {
        return "auth/resetare-parola";
    }

    // Procesează email-ul și trimite token-ul
    @PostMapping("/cere-resetare")
    public String cereResetare(@RequestParam String email, Model model) {
        return utilizatoriRepository.findByEmail(email)
                .map(utilizator -> {
                    resetareParolaService.genereazaSiTrimiteToken(utilizator);
                    model.addAttribute("message", "Link-ul a fost trimis pe email.");
                    return "redirect:/login";
                })
                .orElseGet(() -> {
                    // Fluxul [A1]: Rămânem pe pagină, afișăm eroarea
                    model.addAttribute("error", "Adresa de email nu a fost găsită în sistem.");
                    return "auth/resetare-parola"; // Returnăm direct numele template-ului
                });
    }


    @GetMapping("/schimba-view")
    public String schimbaParolaView(@RequestParam String token, Model model, RedirectAttributes redirectAttributes) {

        // Verificăm existata si validitate token
        boolean esteValid = resetareParolaService.esteTokenValid(token);

        if (!esteValid) {
            // Token invalid sau expirat
            redirectAttributes.addFlashAttribute("error", "Token invalid sau expirat. Vă rugăm să cereți un link nou.");
            return "redirect:/resetare-parola/cere-resetare-view";
        }

        // Token valid
        model.addAttribute("token", token);
        return "auth/resetare-parola-schimba";
    }

    // Procesează parola nouă
    @PostMapping("/schimba")
    public String schimbaParola(@RequestParam String token,
                                @RequestParam String parolaNoua,
                                RedirectAttributes redirectAttributes) {

        if (!PasswordValidator.isValida(parolaNoua)) {
            redirectAttributes.addFlashAttribute("error", "Parola trebuie să aibă cel puțin 8 caractere, o cifră și un caracter special.");
            redirectAttributes.addAttribute("token", token); // Obligatoriu: reatașează token-ul în URL-ul de redirect
            return "redirect:/resetare-parola/schimba-view";
        }

        boolean succes = resetareParolaService.reseteazaParola(token, parolaNoua);

        if (succes) {
            redirectAttributes.addFlashAttribute("message", "Parola a fost schimbată!");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Token invalid sau expirat.");
            return "redirect:/login";
        }
    }
}
