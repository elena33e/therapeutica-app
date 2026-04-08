package com.therapeutica.therapeutica_app.analize_medicale.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuletinEditabilDTO {
    private UUID documentId;
    private UUID pacientId;

    @Builder.Default
    // Specificăm tipul concret LinkedHashMap în loc de interfața Map
    private LinkedHashMap<String, SectiuneWrapperDTO> sectiuni = new LinkedHashMap<>();
}

