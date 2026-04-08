package com.therapeutica.therapeutica_app.pacienti;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PacientiDTO {

    private UUID id;

    @NotNull
    private UUID user;

    @NotNull
    private UUID medic;
}
