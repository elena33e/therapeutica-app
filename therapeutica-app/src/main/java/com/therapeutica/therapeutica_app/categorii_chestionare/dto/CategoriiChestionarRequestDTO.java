package com.therapeutica.therapeutica_app.categorii_chestionare.dto;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CategoriiChestionarRequestDTO {
    private String nume;
    private UUID chestionarId;
}
