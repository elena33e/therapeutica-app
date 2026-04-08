package com.therapeutica.therapeutica_app.auth.dto;


import lombok.Data;

@Data
public class LogoutResponse {
    private boolean success;
    private String message;

    public LogoutResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}