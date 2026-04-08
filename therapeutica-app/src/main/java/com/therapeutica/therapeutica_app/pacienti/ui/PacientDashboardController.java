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

        // Verifică dacă utilizatorul este autentificat și este pacient
        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null || userRole == null || !userRole.equals("PACIENT")) {
            return "redirect:/login";
        }

        // Verifică dacă ID-ul din URL corespunde cu cel din sesiune
        if (!id.equals(userId)) {
            return "redirect:/pacient/dashboard/" + userId;
        }

        // Adaugă atributele în model
        model.addAttribute("pacientId", userId);
        model.addAttribute("userNume", session.getAttribute("userNume"));
        model.addAttribute("userPrenume", session.getAttribute("userPrenume"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userRole", userRole);

        // Adaugă URI-ul curent în model
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", "Dashboard Pacient");

        return "pacient/dashboard";
    }

//    @GetMapping("/profil/{id}")
//    public String pacientProfil(@PathVariable String id,
//                                HttpSession session,
//                                HttpServletRequest request,
//                                Model model) {
//
//        // Verifică dacă utilizatorul este autentificat și este pacient
//        String userId = (String) session.getAttribute("userId");
//        String userRole = (String) session.getAttribute("userRole");
//
//        if (userId == null || userRole == null || !userRole.equals("PACIENT")) {
//            return "redirect:/login";
//        }
//
//        if (!id.equals(userId)) {
//            return "redirect:/pacient/profil/" + userId;
//        }
//
//        model.addAttribute("pacientId", userId);
//        model.addAttribute("userNume", session.getAttribute("userNume"));
//        model.addAttribute("userPrenume", session.getAttribute("userPrenume"));
//        model.addAttribute("userEmail", session.getAttribute("userEmail"));
//        model.addAttribute("userRole", userRole);
//        model.addAttribute("currentUri", request.getRequestURI());
//        model.addAttribute("pageTitle", "Profil Pacient");
//
//        return "pacient/profil";
//    }

    @GetMapping("/chestionare/{id}")
    public String pacientChestionare(@PathVariable String id,
                                     HttpSession session,
                                     HttpServletRequest request,
                                     Model model) {

        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null || userRole == null || !userRole.equals("PACIENT")) {
            return "redirect:/login";
        }

        if (!id.equals(userId)) {
            return "redirect:/pacient/chestionare/" + userId;
        }

        model.addAttribute("pacientId", userId);
        model.addAttribute("userNume", session.getAttribute("userNume"));
        model.addAttribute("userPrenume", session.getAttribute("userPrenume"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userRole", userRole);
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
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null || userRole == null || !userRole.equals("PACIENT")) {
            return "redirect:/login";
        }

        if (!id.equals(userId)) {
            return "redirect:/pacient/documente/" + userId;
        }

        model.addAttribute("pacientId", userId);
        model.addAttribute("userNume", session.getAttribute("userNume"));
        model.addAttribute("userPrenume", session.getAttribute("userPrenume"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userRole", userRole);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", "Documente Medicale");

        return "pacient/documente";
    }

    // Adaugă metodele restante similare...
}