package com.therapeutica.therapeutica_app.security;

import com.therapeutica.therapeutica_app.auth.dto.LoginResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();

        // Extragem datele pe care le-am salvat în Provider
        LoginResponse loginData = (LoginResponse) authentication.getDetails();

        // Populăm sesiunea pentru frontend
        session.setAttribute("accessToken", loginData.getAccessToken());
        session.setAttribute("refreshToken", loginData.getRefreshToken());
        session.setAttribute("userId", loginData.getUserId());
        session.setAttribute("userRole", loginData.getRol().name());
        session.setAttribute("userNume", loginData.getNume());
        session.setAttribute("userPrenume", loginData.getPrenume());
        session.setAttribute("userEmail", authentication.getName());
        session.setMaxInactiveInterval(3600);

        // Redirecționare dinamică bazată pe rol
        String role = loginData.getRol().name().toUpperCase();
        if (role.equals("MEDIC")) {
            response.sendRedirect("/medic/dashboard/" + loginData.getUserId());
        } else if (role.equals("PACIENT")) {
            response.sendRedirect("/pacient/dashboard/" + loginData.getUserId());
        } else {
            response.sendRedirect("/");
        }
    }
}