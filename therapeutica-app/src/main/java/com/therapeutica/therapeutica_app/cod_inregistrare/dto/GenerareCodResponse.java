package com.therapeutica.therapeutica_app.cod_inregistrare.dto;

import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor // Esențial pentru Jackson (JSON)
@AllArgsConstructor
public class GenerareCodResponse {
    private String codUnic;
    private String emailDestinatar;
    private CodInregistrare.StatusCod status;
    private String mesaj;
    private String whatsappLink;


    public GenerareCodResponse(String mesajEroare) {
        this.mesaj = mesajEroare;
    }
}