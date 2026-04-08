package com.therapeutica.therapeutica_app.supabase.dto;
import lombok.Data;

@Data
public class SupabaseAuthResponse {
    private SupabaseUser user;
    private String access_token;
    private String refresh_token;
    private String expires_in;
    private String token_type;
    private AuthError error;

    public boolean isSuccess() {
        return user != null && access_token != null;
    }

    public boolean isNecesitaConfirmareEmail() {
        // Pentru moment, presupunem că întotdeauna necesită confirmare
        // Poți ajusta această logică bazându-te pe răspunsul real de la Supabase
        return true;
    }
}
