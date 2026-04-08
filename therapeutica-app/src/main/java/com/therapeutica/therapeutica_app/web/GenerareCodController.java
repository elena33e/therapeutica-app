
package com.therapeutica.therapeutica_app.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;  // ← ADAUGĂ ASTA
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GenerareCodController {

    @GetMapping("/generare-cod")
    public String paginaGenerareCod(HttpSession session) {
        System.out.println("=== GENERARE COD ENDPOINT HIT ===");

        String userRole = (String) session.getAttribute("userRole");
        String userId = (String) session.getAttribute("userId");

        System.out.println("User ID: " + userId);
        System.out.println("User Role: " + userRole);

        if (userId == null) {
            System.out.println("Redirecting to login");
            return "redirect:/login?redirect=/generare-cod";
        }

        if (!"MEDIC".equals(userRole) && !"ADMINISTRATOR".equals(userRole)) {
            System.out.println("Access denied for role: " + userRole);
            return "redirect:/dashboard?error=access-denied";
        }

        System.out.println("Returning generare-cod template");
        return "generare-cod";
    }
}