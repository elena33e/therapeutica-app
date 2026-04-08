package com.therapeutica.therapeutica_app.auth.dto;

import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import lombok.Data;

@Data
public class LoginResponse {
    private boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
    private RoleType rol;
    private String userId;
    private String nume;
    private String prenume;

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, String accessToken, String refreshToken,
                         RoleType rol, String userId, String nume, String prenume) {
        this.success = success;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.rol = rol;
        this.userId = userId;
        this.nume = nume;
        this.prenume = prenume;
        this.message = "Autentificare reușită!";
    }
}
