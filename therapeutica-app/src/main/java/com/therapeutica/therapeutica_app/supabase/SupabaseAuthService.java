package com.therapeutica.therapeutica_app.supabase;

import com.therapeutica.therapeutica_app.supabase.dto.*;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.therapeutica.therapeutica_app.auth.dto.*;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;

import java.util.Map;
import java.util.Optional;

@Service
public class SupabaseAuthService {

    private final WebClient webClient;
    private final String supabaseUrl;

    @Autowired
    private UtilizatoriRepository utilizatoriRepository;

    public SupabaseAuthService(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.key}") String supabaseKey,
                               UtilizatoriRepository utilizatoriRepository) {
        this.supabaseUrl = supabaseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/auth/v1")
                .defaultHeader("apikey", supabaseKey)
                .defaultHeader("Authorization", "Bearer " + supabaseKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public SupabaseAuthResponse signUp(String email, String password, RoleType rol, String nume, String prenume) {
        try {
            System.out.println("=== SUPABASE SIGNUP START ===");
            System.out.println("Email: " + email);
            System.out.println("Rol: " + rol);
            System.out.println("Nume: " + nume);
            System.out.println("Prenume: " + prenume);

            SupabaseSignUpRequest request = new SupabaseSignUpRequest(email, password, rol, nume, prenume);

            SupabaseAuthResponse response = webClient.post()
                    .uri("/signup")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        System.out.println("Supabase error response: " + errorBody);
                                        return new RuntimeException("Supabase Auth Error: " + errorBody);
                                    }))
                    .bodyToMono(SupabaseAuthResponse.class)
                    .doOnSuccess(authResponse -> {
                        System.out.println("Supabase signup success: " + authResponse.isSuccess());
                        if (authResponse.getError() != null) {
                            System.out.println("Supabase error: " + authResponse.getError().getMessage());
                        }
                        if (authResponse.getUser() != null) {
                            System.out.println("User ID: " + authResponse.getUser().getId());
                        }
                    })
                    .doOnError(throwable -> {
                        System.out.println("Supabase signup error: " + throwable.getMessage());
                    })
                    .block();

            System.out.println("=== SUPABASE SIGNUP COMPLETED ===");
            return response;

        } catch (Exception e) {
            System.out.println("=== EXCEPTION IN SUPABASE SIGNUP ===");
            e.printStackTrace();

            // Creează un răspuns de eroare manual
            SupabaseAuthResponse errorResponse = new SupabaseAuthResponse();
            AuthError error = new AuthError();
            error.setMessage("Supabase service error: " + e.getMessage());
            errorResponse.setError(error);

            return errorResponse;
        }
    }

    public SupabaseUser getUser(String accessToken) {
        try {
            return webClient.get()
                    .uri("/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(SupabaseUser.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    public LoginResponse signIn(String email, String password) {
        try {
            System.out.println("=== SUPABASE SIGNIN START ===");
            System.out.println("Email: " + email);

            Map<String, String> requestBody = Map.of(
                    "email", email,
                    "password", password
            );

            SupabaseAuthResponse response = webClient.post()
                    .uri("/token?grant_type=password")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        System.out.println("Supabase login error: " + errorBody);
                                        return new RuntimeException("Login error: " + errorBody);
                                    }))
                    .bodyToMono(SupabaseAuthResponse.class)
                    .doOnSuccess(authResponse -> {
                        System.out.println("Supabase login successful: " + (authResponse.getAccess_token() != null));
                        if (authResponse.getUser() != null) {
                            System.out.println("Supabase user ID: " + authResponse.getUser().getId());
                        }
                    })
                    .block();

            if (response != null && response.getAccess_token() != null) {
                System.out.println("=== LOOKING FOR USER IN LOCAL DATABASE ===");

                // 1. Caută utilizatorul în baza ta locală
                Optional<Utilizatori> utilizatorOpt = utilizatoriRepository.findByEmail(email);

                if (utilizatorOpt.isPresent()) {
                    Utilizatori utilizator = utilizatorOpt.get();

                    System.out.println("✅ User found in local database:");
                    System.out.println("   ID: " + utilizator.getId());
                    System.out.println("   Nume: " + utilizator.getNume());
                    System.out.println("   Prenume: " + utilizator.getPrenume());
                    System.out.println("   Rol: " + utilizator.getRol());

                    // 2. Returnează răspunsul cu toate informațiile
                    return new LoginResponse(
                            true,
                            response.getAccess_token(),
                            response.getRefresh_token(),
                            utilizator.getRol(),
                            utilizator.getId().toString(),  // ✅ ID-ul din baza locală
                            utilizator.getNume(),           // ✅ Numele
                            utilizator.getPrenume()         // ✅ Prenumele
                    );

                } else {
                    System.out.println("⚠️ User NOT found in local database for email: " + email);

                    // 3. Dacă nu există în baza locală, folosește datele de la Supabase
                    // sau creează utilizatorul în baza locală
                    String supabaseUserId = response.getUser() != null ? response.getUser().getId() : null;

                    // Obține rolul din metadata Supabase sau folosește default
                    String roleFromMetadata = "PACIENT";
                    if (response.getUser() != null && response.getUser().getUser_metadata() != null) {
                        roleFromMetadata = (String) response.getUser().getUser_metadata()
                                .getOrDefault("role", "PACIENT");
                    }

                    RoleType roleType;
                    try {
                        roleType = RoleType.valueOf(roleFromMetadata.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        roleType = RoleType.PACIENT; // Fallback
                    }

                    System.out.println("Using Supabase data:");
                    System.out.println("   Supabase User ID: " + supabaseUserId);
                    System.out.println("   Role from metadata: " + roleType);

                    return new LoginResponse(
                            true,
                            response.getAccess_token(),
                            response.getRefresh_token(),
                            roleType,
                            supabaseUserId,      // ✅ ID-ul de la Supabase
                            "Utilizator",        // ✅ Nume default
                            ""                   // ✅ Prenume default (gol)
                    );
                }

            } else {
                System.out.println("❌ No access token in response");
                return new LoginResponse(false, "Email sau parolă incorecte");
            }

        } catch (Exception e) {
            System.out.println("=== EXCEPTION IN SIGNIN ===");
            e.printStackTrace();
            return new LoginResponse(false, "Eroare la autentificare: " + e.getMessage());
        }
    }

    public LogoutResponse signOut(String accessToken) {
        try {
            System.out.println("SUPABASE SIGNOUT START");
            String cleanToken = accessToken != null ? accessToken.replace("Bearer ", "").trim() : "";

            if (cleanToken.isEmpty()) {
                return new LogoutResponse(false, "Token lipsă");
            }

            String response = webClient.post()
                    .uri("/logout")
                    .header("Authorization", "Bearer " + cleanToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        System.out.println("Supabase logout error: " + errorBody);
                                        return new RuntimeException("Logout error: " + errorBody);
                                    }))
                    .bodyToMono(String.class)
                    .doOnSuccess(result -> {
                        System.out.println("Logout successful at Supabase level");
                    })
                    .doOnError(throwable -> {
                        System.out.println("Logout network error: " + throwable.getMessage());
                    })
                    .block();

            System.out.println("=== SUPABASE SIGNOUT COMPLETED ===");
            return new LogoutResponse(true, "Logout reușit");

        } catch (Exception e) {
            System.out.println("=== EXCEPTION IN SUPABASE SIGNOUT ===");
            e.printStackTrace();
            return new LogoutResponse(false, "Eroare la logout: " + e.getMessage());
        }
    }

    /*public LoginResponse refreshToken(String refreshToken) {
        try {
            System.out.println("=== SUPABASE REFRESH TOKEN START ===");

            Map<String, String> requestBody = Map.of(
                    "refresh_token", refreshToken
            );

            SupabaseAuthResponse response = webClient.post()
                    .uri("/token?grant_type=refresh_token")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        System.out.println("Refresh token error: " + errorBody);
                                        return new RuntimeException("Refresh token error: " + errorBody);
                                    }))
                    .bodyToMono(SupabaseAuthResponse.class)
                    .doOnSuccess(authResponse -> {
                        System.out.println("Token refresh successful: " + (authResponse.getAccess_token() != null));
                    })
                    .block();

            if (response != null && response.getAccess_token() != null) {
                // Obține rolul din user_metadata
                String role = "USER"; // Default
                if (response.getUser() != null && response.getUser().getUser_metadata() != null) {
                    role = (String) response.getUser().getUser_metadata().getOrDefault("role", "USER");
                }
                RoleType roleType = RoleType.valueOf(role);

                return new LoginResponse(
                        true,
                        response.getAccess_token(),
                        response.getRefresh_token(),
                        roleType,
                        supabaseUserId,      // ✅ ID-ul de la Supabase
                        "Utilizator",        // ✅ Nume default
                        ""                   // ✅ Prenume default (gol)
                );
            } else {
                return new LoginResponse(false, "Refresh token invalid sau expirat");
            }

        } catch (Exception e) {
            System.out.println("Refresh token exception: " + e.getMessage());
            return new LoginResponse(false, "Eroare la refresh token: " + e.getMessage());
        }
    }*/

}