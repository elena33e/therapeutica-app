package com.therapeutica.therapeutica_app.auth.dto;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InregistrareResponse {
    private boolean success;
    private String message;
    private String email;
    private RoleType rol;
    private boolean necesitaConfirmareEmail; // CÂMP NOU

    // Constructor pentru succes fără confirmare
    public InregistrareResponse(boolean success, String message, String email, RoleType rol) {
        this(success, message, email, rol, false);
    }

    // Constructor complet
    public InregistrareResponse(boolean success, String message, String email, RoleType rol, boolean necesitaConfirmareEmail) {
        this.success = success;
        this.message = message;
        this.email = email;
        this.rol = rol;
        this.necesitaConfirmareEmail = necesitaConfirmareEmail;
    }

    // Constructor pentru eroare
    public InregistrareResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.email = null;
        this.rol = null;
        this.necesitaConfirmareEmail = false;
    }

}
