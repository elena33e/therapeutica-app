package com.therapeutica.therapeutica_app.analize_medicale.dto;

import lombok.*;
import java.util.*;


// TREBUIE SA FIE PUBLICĂ pentru a fi văzută de Jackson și Thymeleaf
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectiuneWrapperDTO {
    private List<IndicatorDTO> indicatori = new ArrayList<>();
}
