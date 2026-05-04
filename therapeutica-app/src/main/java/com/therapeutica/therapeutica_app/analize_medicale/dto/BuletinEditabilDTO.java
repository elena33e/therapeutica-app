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
    private List<SectiuneWrapperDTO> sectiuni = new ArrayList<>();
}

