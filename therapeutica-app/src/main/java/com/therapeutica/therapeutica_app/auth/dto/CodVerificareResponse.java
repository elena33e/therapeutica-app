package com.therapeutica.therapeutica_app.auth.dto;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodVerificareResponse {
    private boolean valid;
    private String message;
    private String email;
    private String nume;
    private String prenume;
    private RoleType rol;

    public CodVerificareResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public CodVerificareResponse(boolean valid, String message, String email, String nume, String prenume, RoleType rol) {
        this.valid = valid;
        this.message = message;
        this.email = email;
        this.nume = nume;
        this.prenume = prenume;
        this.rol = rol;
    }
}
