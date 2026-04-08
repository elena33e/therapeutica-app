package com.therapeutica.therapeutica_app.security;

import com.therapeutica.therapeutica_app.auth.dto.LoginResponse;
import com.therapeutica.therapeutica_app.supabase.SupabaseAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupabaseAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private SupabaseAuthService supabaseAuthService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Apelăm Supabase direct din filtrul de securitate
        LoginResponse response = supabaseAuthService.signIn(email, password);

        if (!response.isSuccess()) {
            throw new BadCredentialsException("Email sau parolă incorecte");
        }

        String roleString = "ROLE_" + response.getRol().name().toUpperCase();
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleString));

        // Returnăm un token care conține și ID-ul utilizatorului + restul datelor ca "details"
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, password, authorities);
        auth.setDetails(response); // Salvăm obiectul complet pentru a-l folosi la pasul următor

        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}