package com.therapeutica.therapeutica_app.notificari.events;

import java.util.UUID;


public record NotificareEvent(
        UUID destinatarId,
        String titlu,
        String mesaj,
        String linkActiune
) {}