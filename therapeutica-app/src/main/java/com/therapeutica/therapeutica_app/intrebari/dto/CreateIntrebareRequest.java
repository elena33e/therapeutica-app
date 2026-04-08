package com.therapeutica.therapeutica_app.intrebari.dto;

import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateIntrebareRequest {

    @NotNull
    private UUID categorieId;

    @NotBlank(message = "Textul întrebării este obligatoriu")
    private String textIntrebare;

    @NotNull
    private Intrebare.TipIntrebare tipIntrebare;

    private Integer ordine = 0;
    private Boolean obligatorie = true;
    private List<String> optiuni; // Pentru MULTIPLE_CHOICE
}