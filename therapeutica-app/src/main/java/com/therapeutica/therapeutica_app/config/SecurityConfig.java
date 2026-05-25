package com.therapeutica.therapeutica_app.config;

import com.therapeutica.therapeutica_app.security.LoginSuccessHandler;
import com.therapeutica.therapeutica_app.security.SupabaseAuthenticationProvider;
import com.therapeutica.therapeutica_app.supabase.SupabaseLogoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SupabaseLogoutHandler supabaseLogoutHandler;

    @Autowired
    private SupabaseAuthenticationProvider supabaseAuthenticationProvider;

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // -Permitem iframe-urile din interiorul aplicației
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )


                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/inregistrare",
                                "/api/auth/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error"
                        ).permitAll()
                        // Spring Security adaugă automat prefixul ROLE_ când verifică hasRole
                        // Deci "MEDIC" în hasRole înseamnă "ROLE_MEDIC" în Provider
                        .requestMatchers("/medic/**").hasRole("MEDIC")
                        .requestMatchers("/pacient/**").hasRole("PACIENT")
                        .anyRequest().authenticated()
                )
                // OBLIGATORIU: Înregistrează provider-ul care vorbește cu Supabase
                .authenticationProvider(supabaseAuthenticationProvider)

                .formLogin(login -> login
                        .loginPage("/login")
                        // OBLIGATORIU: Spune-i unde să asculte POST-ul de la formular
                        .loginProcessingUrl("/process-login")
                        // OBLIGATORIU: Spune-i cine gestionează succesul (redirecționarea la dashboard)
                        .successHandler(loginSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout-web")
                        .addLogoutHandler(supabaseLogoutHandler)
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}