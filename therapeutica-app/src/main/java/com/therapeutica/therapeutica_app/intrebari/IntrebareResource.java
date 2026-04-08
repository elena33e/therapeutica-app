package com.therapeutica.therapeutica_app.intrebari;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class IntrebareResource {
    private UUID id;
    private String text;
    private String tip;
    private Boolean obligatorie;
    private Integer ordine;
    private List<String> optiuni; // multiple choice

    // Pentru răspunsuri (când se completează chestionarul)
    private Object raspuns;
    private Boolean validat;
    private String eroare;
}