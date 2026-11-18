package com.therapeutica.therapeutica_app.raspunsuri_intrebari;

import com.therapeutica.therapeutica_app.notificari.events.NotificareEvent;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.raport_chestionar.ScoringService;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.security.AccesChestionarService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalvareChestionarService {

    private final RaspunsuriChestionareRepository chestionareRepository;
    private final AccesChestionarService accesChestionarService;
    private final RaspunsuriIntrebariService raspunsuriIntrebariService;
    private final ScoringService scoringService;
    private final ApplicationEventPublisher eventPublisher;

    public record RezultatSalvare(
            UUID pacientUserId,
            UUID pacientId,
            String numeChestionar
    ) {}

    @Transactional
    public RezultatSalvare salveaza(
            UUID raspunsChestionarId,
            Map<String, String> raspunsuriValidate,
            HttpSession session) {

        // Blocăm atribuirea până la încheierea tranzacției.
        var atribuire = chestionareRepository
                .findByIdForUpdate(raspunsChestionarId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Chestionarul nu a fost găsit."
                        )
                );

        // Reconfirmăm accesul și statusul după obținerea blocării.
        accesChestionarService.verificaModificare(
                session,
                raspunsChestionarId
        );

        Pacienti pacient = atribuire.getPacient();

        if (pacient == null
                || pacient.getUser() == null
                || atribuire.getChestionar() == null) {
            throw new NotFoundException(
                    "Datele asociate chestionarului sunt incomplete."
            );
        }

        UUID pacientUserId = pacient.getUser().getId();
        UUID pacientId = pacient.getId();
        String numeChestionar = atribuire.getChestionar().getNume();

        raspunsuriIntrebariService.proceseazaRaspunsuri(
                raspunsChestionarId,
                raspunsuriValidate
        );

        scoringService.calculeazaSiSalveazaScor(
                raspunsChestionarId
        );

        notificaMedicul(
                pacient,
                raspunsChestionarId,
                numeChestionar
        );

        return new RezultatSalvare(
                pacientUserId,
                pacientId,
                numeChestionar
        );
    }

    private void notificaMedicul(
            Pacienti pacient,
            UUID raspunsChestionarId,
            String numeChestionar) {

        if (pacient.getMedic() == null
                || pacient.getMedic().getUser() == null) {
            return;
        }

        UUID medicUserId = pacient.getMedic().getUser().getId();
        String numePacient = pacient.getUser().getNume();

        eventPublisher.publishEvent(new NotificareEvent(
                medicUserId,
                "Chestionar completat: " + numeChestionar,
                "Pacientul " + numePacient
                        + " a finalizat completarea chestionarului.",
                "/chestionare/medic/vizualizare/"
                        + raspunsChestionarId
        ));
    }
}