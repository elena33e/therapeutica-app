package com.therapeutica.therapeutica_app.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;


@Getter
@AllArgsConstructor
public class BeforeDeleteMedici {

    private UUID id;

}
