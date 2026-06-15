package com.therapeutica.therapeutica_app.security;

import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilizatoriRepository utilizatoriRepository;

    public CustomUserDetailsService(UtilizatoriRepository utilizatoriRepository) {
        this.utilizatoriRepository = utilizatoriRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilizatori utilizator = utilizatoriRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizatorul cu email-ul " + email + " nu există."));

        String rolCuPrefix = "ROLE_" + utilizator.getRol().name();

        return new User(
                utilizator.getEmail(),
                utilizator.getParola(),
                Collections.singletonList(new SimpleGrantedAuthority(rolCuPrefix))
        );
    }
}