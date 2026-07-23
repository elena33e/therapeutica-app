package com.therapeutica.therapeutica_app.audit;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Scrie intrari in jurnalul de audit.
 * Esecul scrierii nu blocheaza niciodata fluxul aplicatiei.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inregistreaza(UUID utilizatorId,
                              AuditLog.ActiuneAudit actiune,
                              AuditLog.TipResursa tipResursa,
                              UUID resursaId,
                              UUID pacientId) {
        try {
            AuditLog intrare = AuditLog.builder()
                    .utilizatorId(utilizatorId)
                    .actiune(actiune)
                    .tipResursa(tipResursa)
                    .resursaId(resursaId)
                    .pacientId(pacientId)
                    .adresaIp(extrageAdresaIp())
                    .build();

            auditLogRepository.save(intrare);

        } catch (Exception e) {
            // Auditul nu trebuie sa opreasca niciodata fluxul aplicatiei
            log.error("Eroare la scrierea in jurnalul de audit (utilizator={}, actiune={}, resursa={}): {}",
                    utilizatorId, actiune, resursaId, e.getMessage());
        }
    }

    private String extrageAdresaIp() {
        ServletRequestAttributes atribute =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (atribute == null) {
            // Nu exista context HTTP (ex. thread @Async, job programat)
            return null;
        }

        return atribute.getRequest().getRemoteAddr();
    }

    /**
     * Extrage id-ul utilizatorului din sesiunea curenta.
     * Returneaza null daca nu exista sesiune sau atributul lipseste.
     */
    public UUID utilizatorCurent(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object valoare = session.getAttribute("userId");
        if (valoare == null) {
            return null;
        }

        if (valoare instanceof UUID) {
            return (UUID) valoare;
        }

        try {
            return UUID.fromString(valoare.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Atributul userId din sesiune nu este un UUID valid: {}", valoare);
            return null;
        }
    }
}