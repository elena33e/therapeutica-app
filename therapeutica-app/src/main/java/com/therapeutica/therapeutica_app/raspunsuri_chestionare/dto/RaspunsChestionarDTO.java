package com.therapeutica.therapeutica_app.raspunsuri_chestionare.dto;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RaspunsChestionarDTO {
    private UUID id;
    private UUID pacientId;
    private UUID medicId;
    private UUID chestionarId;
    private String status;
    private LocalDateTime completatLa;
    private LocalDateTime atribuitLa;
    private BigDecimal scorTotalGeneral;
    private String numePacient;
    private String numeMedic;
    private String numeChestionar;
}