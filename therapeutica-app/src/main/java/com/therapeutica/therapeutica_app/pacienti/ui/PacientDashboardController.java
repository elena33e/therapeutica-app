package com.therapeutica.therapeutica_app.pacienti.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/pacient")
public class PacientDashboardController {

    @GetMapping("/dashboard/{id}")
    public String pacientDashboard(@PathVariable String id,
                                   HttpSession session,
                                   HttpServletRequest request,
                                   Model model) {

        String loggedInUserId = (String) session.getAttribute("userId");

        // Rămâne doar verificarea de siguranță pentru ID-ul din URL
        if (!id.equals(loggedInUserId)) {
            return "redirect:/pacient/dashboard/" + loggedInUserId;
        }

        // Populam modelul direct din sesiune pentru afișarea în Thymeleaf
        model.addAttribute("pacientId", loggedInUserId);
        model.addAttribute("userNume", session.getAttribute("userNume"));
        model.addAttribute("userPrenume", session.getAttribute("userPrenume"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", "Dashboard Pacient");

        return "pacient/dashboard";
    }



    @GetMapping("/chestionare/{id}")
    public String pacientChestionare(@PathVariable String id,
                                     HttpSession session,
                                     HttpServletRequest request,
                                     Model model) {

        String userId = (String) session.getAttribute("userId");

        if (!id.equals(userId)) {
            return "redirect:/pacient/chestionare/" + userId;
        }

        model.addAttribute("pacientId", userId);
        model.addAttribute("userNume", session.getAttribute("userNume"));
        model.addAttribute("userPrenume", session.getAttribute("userPrenume"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", "Chestionare Medicale");

        return "pacient/chestionare";
    }

    @GetMapping("/documente/{id}")
    public String pacientDocumente(@PathVariable String id,
                                   HttpSession session,
                                   HttpServletRequest request,
                                   Model model) {

        String userId = (String) session.getAttribute("userId");


        if (!id.equals(userId)) {
            return "redirect:/pacient/documente/" + userId;
        }

        model.addAttribute("pacientId", userId);
        model.addAttribute("userNume", session.getAttribute("userNume"));
        model.addAttribute("userPrenume", session.getAttribute("userPrenume"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", "Documente Medicale");

        return "pacient/documente";
    }

}