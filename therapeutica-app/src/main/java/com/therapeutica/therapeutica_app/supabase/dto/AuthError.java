
package com.therapeutica.therapeutica_app.supabase.dto;

import lombok.Data;

@Data
public class AuthError {
    private String message;
    private String status;
}