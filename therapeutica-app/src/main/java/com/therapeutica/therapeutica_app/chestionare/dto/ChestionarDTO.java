package com.therapeutica.therapeutica_app.chestionare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ChestionarDTO {

    private UUID id;

    @NotBlank(message = "Numele chestionarului este obligatoriu")
    @Size(max = 100, message = "Numele nu poate depăși 100 de caractere")
    private String nume;

    @Size(max = 2000, message = "Descrierea nu poate depăși 2000 de caractere")
    private String descriere;

    @Size(max = 2000, message = "Instrucțiunile nu pot depăși 2000 de caractere")
    private String instructiuni;

    private LocalDateTime creatLa;

    private int numarCategorii;
}