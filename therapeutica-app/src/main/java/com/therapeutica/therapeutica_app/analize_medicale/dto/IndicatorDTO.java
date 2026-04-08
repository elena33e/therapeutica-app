package com.therapeutica.therapeutica_app.analize_medicale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorDTO {
    private String nume;
    private String valoare;
    private String um;
    private String interval;
    private String metoda_detectata;
    private String loincNum;  // Codul tehnic (trimis la Python)
    private String loincNume;

    private List<LoincOptiuneDTO> candidati = new ArrayList<>();
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class LoincOptiuneDTO {
    private String code;
    private String name;
}