package com.therapeutica.therapeutica_app.security;

import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariRepository;
import com.therapeutica.therapeutica_app.util.AccesInterzisException;
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
@Transactional(readOnly = true)
public class AccesChestionarService {

    private final RaspunsuriChestionareRepository chestionareRepository;
    private final RaspunsuriIntrebariRepository raspunsuriRepository;
    private final AccesPacientService accesPacientService;

    public RaspunsuriChestionare incarcaPentruPacient(
            HttpSession session,
            UUID raspunsChestionarId) {

        RaspunsuriChestionare chestionar = chestionareRepository
                .findByIdForCompletare(raspunsChestionarId)
                .orElseThrow(() ->
                        new NotFoundException("Chestionarul nu a fost găsit.")
                );

        if (chestionar.getPacient() == null) {
            throw new AccesInterzisException(
                    "Chestionarul nu are un pacient asociat."
            );
        }

        accesPacientService.verificaAccesPropriu(
                session,
                chestionar.getPacient().getId()
        );

        return chestionar;
    }

    public void verificaModificare(
            HttpSession session,
            UUID raspunsChestionarId) {

        RaspunsuriChestionare chestionar =
                incarcaPentruPacient(session, raspunsChestionarId);

        boolean editabil =
                chestionar.getStatus()
                        == RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT
                        || chestionar.getStatus()
                        == RaspunsuriChestionare.StatusRaspuns.IN_PROGRESS;

        if (!editabil) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chestionarul nu mai poate fi modificat."
            );
        }
    }

    public void verificaStergereRaspuns(
            HttpSession session,
            UUID raspunsIntrebareId) {

        RaspunsuriIntrebari raspuns = raspunsuriRepository
                .findById(raspunsIntrebareId)
                .orElseThrow(() ->
                        new NotFoundException("Răspunsul nu a fost găsit.")
                );

        if (raspuns.getRaspunsChestionar() == null) {
            throw new NotFoundException(
                    "Chestionarul asociat răspunsului nu a fost găsit."
            );
        }

        verificaModificare(
                session,
                raspuns.getRaspunsChestionar().getId()
        );
    }
}