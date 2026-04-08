package com.therapeutica.therapeutica_app.cod_inregistrare.dto;

import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CodInregistrareDTO {

    private UUID id;

    @NotNull
    private String codUnic;

    @NotNull
    private CodInregistrare.StatusCod status;

    private OffsetDateTime createdAt;

    @NotNull
    private UUID generatDe;

    private UUID atribuit;

    @NotNull
    private String emailDestinatar;

    private String cnpDestinatar;

    @NotNull
    private RoleType rolDestinatar;

}
