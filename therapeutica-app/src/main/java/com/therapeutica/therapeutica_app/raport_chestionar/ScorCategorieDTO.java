package com.therapeutica.therapeutica_app.raport_chestionar;


import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ScorCategorieDTO {
    private String numeCategorie;
    private BigDecimal scorMediu;
    private Integer numarIntrebari;
    private BigDecimal scorMinim;
    private BigDecimal scorMaxim;
    private ScalaCromatica interval;
}
