package com.therapeutica.therapeutica_app.raspunsuri_chestionare.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RaspunsChestionarRequestDTO {
    private UUID pacientId;
    private UUID medicId;
    private UUID chestionarId;
    private String status;
    private LocalDateTime completatLa;
    private BigDecimal scorTotalGeneral;
}