
package com.therapeutica.therapeutica_app.supabase.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SupabaseUser {
    private String id;
    private String email;
    private Map<String, Object> user_metadata;
    private String created_at;
    private String updated_at;
}