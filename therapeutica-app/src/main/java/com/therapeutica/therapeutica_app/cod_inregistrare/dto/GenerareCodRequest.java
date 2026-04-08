package com.therapeutica.therapeutica_app.cod_inregistrare.dto;

import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class GenerareCodRequest {
    @NotNull(message = "ID-ul medicului este obligatoriu")
    private UUID medicId;

    @NotBlank(message = "Email-ul destinatarului este obligatoriu")
    @Email(message = "Format email invalid")
    private String emailDestinatar;

    @Size(min = 13, max = 13, message = "CNP-ul trebuie să aibă exact 13 caractere")
    private String cnpDestinatar;

    @NotNull(message = "Rolul destinatarului este obligatoriu")
    private RoleType rolDestinatar;

    @NotBlank(message = "Numele destinatarului este obligatoriu")
    private String numeDestinatar;

    @NotBlank(message = "Prenumele destinatarului este obligatoriu")
    private String prenumeDestinatar;
}
