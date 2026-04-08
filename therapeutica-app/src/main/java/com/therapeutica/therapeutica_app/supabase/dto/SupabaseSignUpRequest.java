// SupabaseSignUpRequest.java
package com.therapeutica.therapeutica_app.supabase.dto;

import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SupabaseSignUpRequest {
    private String email;
    private String password;
    private Map<String, Object> data;

    public SupabaseSignUpRequest(String email, String password, RoleType rol, String nume, String prenume) {
        this.email = email;
        this.password = password;
        this.data = new HashMap<>();
        this.data.put("rol", rol.name());
        this.data.put("nume", nume);
        this.data.put("prenume", prenume);
    }
}