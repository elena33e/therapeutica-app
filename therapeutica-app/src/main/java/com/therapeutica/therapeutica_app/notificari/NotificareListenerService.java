package com.therapeutica.therapeutica_app.notificari;

import com.therapeutica.therapeutica_app.notificari.events.NotificareEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificareListenerService {

    private final NotificareRepository notificareRepository;

    //@Async
    @EventListener
    public void proceseazaNotificare(NotificareEvent event) {
        try {
            Notificare notificare = Notificare.builder()
                    .utilizatorDestinatarId(event.destinatarId())
                    .titlu(event.titlu())
                    .mesaj(event.mesaj())
                    .linkActiune(event.linkActiune())
                    .citita(false)
                    .dataCreare(LocalDateTime.now())
                    .build();

            notificareRepository.save(notificare);
            log.info("Notificare salvată pentru utilizatorul: {}", event.destinatarId());
        } catch (Exception e) {

            log.error("Eroare la salvarea notificării: {}", e.getMessage());
        }
    }
}