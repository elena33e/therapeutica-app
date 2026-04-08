package com.therapeutica.therapeutica_app.intrebari.ui;

import com.therapeutica.therapeutica_app.intrebari.IntrebariService;
import com.therapeutica.therapeutica_app.intrebari.dto.IntrebareDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/intrebari")
@RequiredArgsConstructor
public class IntrebariViewController {

    private final IntrebariService intrebariService;

    @GetMapping("/categorie/{categorieId}")
    public String getIntrebariByCategorie(@PathVariable UUID categorieId, Model model) {
        List<IntrebareDTO> intrebari = intrebariService.getIntrebariByCategorie(categorieId);
        model.addAttribute("intrebari", intrebari);
        model.addAttribute("categorieId", categorieId);
        return "admin/intrebari/list";
    }

    @GetMapping("/chestionar/{chestionarId}")
    public String getIntrebariByChestionar(@PathVariable UUID chestionarId, Model model) {
        List<IntrebareDTO> intrebari = intrebariService.getIntrebariByChestionar(chestionarId);
        model.addAttribute("intrebari", intrebari);
        model.addAttribute("chestionarId", chestionarId);
        return "admin/intrebari/chestionar-list";
    }
}