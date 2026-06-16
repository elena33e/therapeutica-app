package com.therapeutica.therapeutica_app.diagnostic;

import com.therapeutica.therapeutica_app.notificari.events.NotificareEvent;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosticMedicService {

    private final DiagnosticMedicRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DiagnosticMedic salveazaDiagnostic(DiagnosticMedic diagnostic, List<UUID> chestionarIds, List<UUID> analizeIds) {

        if (chestionarIds != null) {
            diagnostic.getChestionareReferinte().clear();
            diagnostic.getChestionareReferinte().addAll(chestionarIds);
        }

        if (analizeIds != null) {
            diagnostic.getAnalizeReferinte().clear();
            diagnostic.getAnalizeReferinte().addAll(analizeIds);
        }

        DiagnosticMedic savedDiagnostic = repository.save(diagnostic);

        // Declanșăm notificarea către pacient (try-catch-ul din interior previne rollback-ul în caz de eroare)
        triggerNotificarePacient(savedDiagnostic);

        return savedDiagnostic;
    }

    public List<DiagnosticMedic> getIstoricDiagnosticPacient(UUID pacientId) {
        return repository.findByPacientId(pacientId);
    }


    public List<DiagnosticMedic> getIstoricDiagnosticPacientDetaliat(UUID pacientId) {
        return repository.findByPacientIdWithFullDetails(pacientId);
    }

    private void triggerNotificarePacient(DiagnosticMedic diagnostic) {
        try {
            Pacienti pacient = diagnostic.getPacient();

            if (pacient != null && pacient.getUser() != null) {
                UUID userId = pacient.getUser().getId();

                String link = "/pacient/istoric-diagnostice";

                String titlu = "Diagnostic nou înregistrat";
                String mesaj = "Medicul ți-a stabilit un nou diagnostic. Accesează istoricul tău medical pentru a vedea detaliile.";

                eventPublisher.publishEvent(new NotificareEvent(userId, titlu, mesaj, link));
                log.debug("Eveniment de notificare pentru diagnostic trimis către UserID: {}", userId);
            } else {
                log.warn("Notificarea nu a putut fi trimisă: Diagnosticul nu este asociat unui pacient valid.");
            }
        } catch (Exception e) {
            log.error("Eroare silențioasă la trimiterea notificării de diagnostic: {}", e.getMessage());
        }
    }
}