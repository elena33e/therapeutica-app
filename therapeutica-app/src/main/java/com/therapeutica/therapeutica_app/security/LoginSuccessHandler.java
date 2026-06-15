package com.therapeutica.therapeutica_app.security;

import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UtilizatoriRepository utilizatoriRepository;

    public LoginSuccessHandler(UtilizatoriRepository utilizatoriRepository) {
        this.utilizatoriRepository = utilizatoriRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();

        User springUser = (User) authentication.getPrincipal();
        String email = springUser.getUsername();

        // Preluăm datele complete ale utilizatorului
        Utilizatori utilizator = utilizatoriRepository.findByEmail(email)
                .orElseThrow(() -> new ServletException("Eroare critică: Utilizatorul autentificat nu există local."));

        // Populăm sesiunea
        session.setAttribute("userId", utilizator.getId().toString());
        session.setAttribute("userRole", utilizator.getRol().name());
        session.setAttribute("userNume", utilizator.getNume());
        session.setAttribute("userPrenume", utilizator.getPrenume());
        session.setAttribute("userEmail", utilizator.getEmail());
        session.setMaxInactiveInterval(3600);

        // Redirecționar role-based
        String role = utilizator.getRol().name().toUpperCase();
        if (role.equals("ADMIN")) {
            response.sendRedirect("/admin/dashboard/" + utilizator.getId());
        } else if (role.equals("MEDIC")) {
            response.sendRedirect("/medic/dashboard/" + utilizator.getId());
        } else if (role.equals("PACIENT")) {
            response.sendRedirect("/pacient/dashboard/" + utilizator.getId());
        } else {
            response.sendRedirect("/");
        }
    }
}