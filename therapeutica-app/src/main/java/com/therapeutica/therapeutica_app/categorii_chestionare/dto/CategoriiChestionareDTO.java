package com.therapeutica.therapeutica_app.categorii_chestionare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CategoriiChestionareDTO {

    private UUID id;

    @NotNull(message = "ID-ul chestionarului este obligatoriu")
    private UUID chestionarId;

    @NotBlank(message = "Numele categoriei este obligatoriu")
    @Size(max = 100, message = "Numele categoriei nu poate depăși 100 de caractere")
    private String nume;

    // Pentru afișare
    private String numeChestionar;
}
