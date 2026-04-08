package com.therapeutica.therapeutica_app.cod_inregistrare.dto;

import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerareCodResponse {
    private String codUnic;
    private String emailDestinatar;
    private CodInregistrare.StatusCod status;
    private String mesaj;

    // Constructor pentru succes
    public GenerareCodResponse(String codUnic, String emailDestinatar) {
        this.codUnic = codUnic;
        this.emailDestinatar = emailDestinatar;
        this.status = CodInregistrare.StatusCod.NEUTILIZAT;
        this.mesaj = "Cod generat cu succes și trimis pe email";
    }

    // Constructor pentru eroare
    public GenerareCodResponse(String mesajEroare) {
        this.mesaj = mesajEroare;
    }
}
