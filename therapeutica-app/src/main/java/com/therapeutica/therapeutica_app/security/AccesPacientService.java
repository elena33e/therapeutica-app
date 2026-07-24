package com.therapeutica.therapeutica_app.security;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.util.AccesInterzisException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Verifica dreptul de acces la datele unui pacient.
 * Spring Security acopera autorizarea pe rol; aici se verifica
 * apartenenta concreta: acest pacient apartine acestui medic?
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccesPacientService {

    private final PacientiRepository pacientiRepository;

    /**
     * Arunca AccesInterzisException daca pacientul nu apartine medicului din sesiune.
     */
    public void verificaAccesMedic(HttpSession session, UUID pacientId) {
        UUID medicUserId = extrageUserId(session);

        if (medicUserId == null) {
            throw new AccesInterzisException("Sesiune invalida");
        }

        Pacienti pacient = pacientiRepository.findByIdWithUser(pacientId)
                .orElseThrow(() -> new AccesInterzisException("Pacientul nu a fost gasit"));

        if (pacient.getMedic() == null || pacient.getMedic().getUser() == null) {
            throw new AccesInterzisException("Pacientul nu are un medic asociat");
        }

        if (!pacient.getMedic().getUser().getId().equals(medicUserId)) {
            log.warn("Acces refuzat: utilizatorul {} a incercat sa acceseze pacientul {}",
                    medicUserId, pacientId);
            throw new AccesInterzisException("Nu aveti acces la datele acestui pacient");
        }
    }

    /**
     * Verifica faptul ca pacientul din sesiune isi acceseaza propriile date.
     */
    public void verificaAccesPropriu(HttpSession session, UUID pacientId) {
        UUID userId = extrageUserId(session);

        if (userId == null) {
            throw new AccesInterzisException("Sesiune invalida");
        }

        Pacienti pacient = pacientiRepository.findByUserId(userId)
                .orElseThrow(() -> new AccesInterzisException("Pacientul nu a fost gasit"));

        if (!pacient.getId().equals(pacientId)) {
            log.warn("Acces refuzat: utilizatorul {} a incercat sa acceseze datele pacientului {}",
                    userId, pacientId);
            throw new AccesInterzisException("Nu aveti acces la aceste date");
        }
    }

    private UUID extrageUserId(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object valoare = session.getAttribute("userId");
        if (valoare == null) {
            return null;
        }

        try {
            return UUID.fromString(valoare.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}