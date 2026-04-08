package com.therapeutica.therapeutica_app.chestionare.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorieVisualDTO {
    private UUID id;
    private String nume;
    private String descriere;
    private Double scorMediu;
    private Integer scorMaxim;
    private Integer procentaj;
    private String culoare;
    private String interpretare;
    private Integer numarIntrebari;
}