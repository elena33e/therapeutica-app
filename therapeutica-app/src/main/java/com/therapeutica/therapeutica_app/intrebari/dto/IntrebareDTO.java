package com.therapeutica.therapeutica_app.intrebari.dto;

import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class IntrebareDTO {
    private UUID id;
    private UUID categorieId;
    private String categorieNume;
    private String textIntrebare;
    private Intrebare.TipIntrebare tipIntrebare;
    private Integer ordine;
    private Boolean obligatorie;
    private List<String> optiuni; // Pentru MULTIPLE_CHOICE
    private String optiuniJson;
    private String hpoCode;
    private String hpoTerm;

    public static IntrebareDTO fromEntity(Intrebare intrebare) {
        IntrebareDTO dto = new IntrebareDTO();
        dto.setId(intrebare.getId());
        dto.setTextIntrebare(intrebare.getTextIntrebare());
        dto.setTipIntrebare(intrebare.getTipIntrebare());
        dto.setOrdine(intrebare.getOrdine());
        dto.setObligatorie(intrebare.getObligatorie());
        dto.setOptiuniJson(intrebare.getOptiuniJson());
        dto.setHpoCode(intrebare.getHpoCode());
        dto.setHpoTerm(intrebare.getHpoTerm());

        if (intrebare.getCategorie() != null) {
            dto.setCategorieId(intrebare.getCategorie().getId());
            dto.setCategorieNume(intrebare.getCategorie().getNume());
        }

        return dto;
    }
}