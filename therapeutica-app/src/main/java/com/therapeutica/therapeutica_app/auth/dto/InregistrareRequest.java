package com.therapeutica.therapeutica_app.auth.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InregistrareRequest {
    private String codUnic;
    private String cnp;
    private String parola;
    private String confirmaParola;
}
