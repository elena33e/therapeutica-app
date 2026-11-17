package com.therapeutica.therapeutica_app.chestionare;

import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.security.AccesPacientService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevizuireChestionarService {

    private final RaspunsuriChestionareRepository chestionareRepository;
    private final AccesPacientService accesPacientService;

    public record RezultatRevizuire(
            UUID pacientId,
            boolean modificat
    ) {}

    @Transactional
    public RezultatRevizuire marcheazaCaRevizuit(
            UUID raspunsChestionarId,
            HttpSession session) {

        RaspunsuriChestionare chestionar = chestionareRepository
                .findByIdForUpdate(raspunsChestionarId)
                .orElseThrow(() ->
                        new NotFoundException("Chestionarul nu a fost găsit.")
                );

        if (chestionar.getPacient() == null) {
            throw new NotFoundException(
                    "Pacientul asociat chestionarului nu a fost găsit."
            );
        }

        UUID pacientId = chestionar.getPacient().getId();

        accesPacientService.verificaAccesMedic(session, pacientId);

        // O cerere repetată nu produce o nouă modificare.
        if (chestionar.getStatus()
                == RaspunsuriChestionare.StatusRaspuns.REVIZUIT) {

            return new RezultatRevizuire(pacientId, false);
        }

        if (chestionar.getStatus()
                != RaspunsuriChestionare.StatusRaspuns.COMPLETAT) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Poate fi revizuit doar un chestionar completat."
            );
        }

        chestionar.setStatus(
                RaspunsuriChestionare.StatusRaspuns.REVIZUIT
        );

        chestionareRepository.save(chestionar);

        return new RezultatRevizuire(pacientId, true);
    }
}