package com.therapeutica.therapeutica_app.supabase; // Modificat aici

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class SupabaseLogoutHandler implements LogoutHandler {

    @Autowired
    private SupabaseAuthService supabaseAuthService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute("accessToken");
            if (token != null) {
                try {
                    System.out.println("Deconectare din Supabase interceptată de Spring Security...");
                    supabaseAuthService.signOut(token);
                } catch (Exception e) {
                    System.err.println("Eroare la invalidarea tokenului în Supabase: " + e.getMessage());
                }
            }
        }
    }
}