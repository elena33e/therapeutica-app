package com.therapeutica.therapeutica_app.utilizatori;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilizatoriDTO {

    private UUID id;

    private OffsetDateTime createdAt; // setat automat, nu obligatoriu la input


    @NotNull
    private String email;

    @NotNull
    private String nume;

    @NotNull
    private String prenume;

    @NotNull
    private RoleType rol;

    private UUID medicId;

    private UUID pacientId;
}
