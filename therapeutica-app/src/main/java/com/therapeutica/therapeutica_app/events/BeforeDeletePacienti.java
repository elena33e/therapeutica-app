package com.therapeutica.therapeutica_app.events;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class BeforeDeletePacienti {

    private UUID id;

}
