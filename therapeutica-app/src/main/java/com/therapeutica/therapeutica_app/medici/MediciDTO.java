package com.therapeutica.therapeutica_app.medici;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediciDTO {

    @NotNull
    private UUID userId;

    @NotNull
    private String specializare;
}
