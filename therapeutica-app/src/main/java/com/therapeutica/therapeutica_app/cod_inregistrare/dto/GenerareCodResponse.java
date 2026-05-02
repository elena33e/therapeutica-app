package com.therapeutica.therapeutica_app.cod_inregistrare.dto;

import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // Esențial pentru Jackson (JSON)
@AllArgsConstructor
public class GenerareCodResponse {
    private String codUnic;
    private String emailDestinatar;
    private CodInregistrare.StatusCod status;

    private String mesaj;


    public GenerareCodResponse(String mesajEroare) {
        this.mesaj = mesajEroare;
    }
}