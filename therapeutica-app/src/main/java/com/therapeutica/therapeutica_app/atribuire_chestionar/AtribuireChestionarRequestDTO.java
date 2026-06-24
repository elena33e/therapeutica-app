package com.therapeutica.therapeutica_app.atribuire_chestionar;


import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AtribuireChestionarRequestDTO {

    @NotNull(message = "ID-ul pacientului este obligatoriu")
    private UUID pacientId;

    @NotNull(message = "ID-ul medicului este obligatoriu")
    private UUID medicId;

    @NotNull(message = "Lista de chestionare este obligatorie")
    private List<UUID> chestionareIds = new ArrayList<>();
}