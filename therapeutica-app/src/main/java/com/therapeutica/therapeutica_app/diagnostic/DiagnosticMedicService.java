package com.therapeutica.therapeutica_app.diagnostic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiagnosticMedicService {

    private final DiagnosticMedicRepository repository;

    @Transactional
    public DiagnosticMedic salveazaDiagnostic(DiagnosticMedic diagnostic, List<UUID> chestionarIds, List<UUID> analizeIds) {
        // Pur și simplu populăm listele din entitate.
        // Hibernate va face INSERT automat în tabelele secundare create de @ElementCollection
        if (chestionarIds != null) {
            diagnostic.getChestionareReferinte().clear();
            diagnostic.getChestionareReferinte().addAll(chestionarIds);
        }

        if (analizeIds != null) {
            diagnostic.getAnalizeReferinte().clear();
            diagnostic.getAnalizeReferinte().addAll(analizeIds);
        }

        return repository.save(diagnostic);
    }

    public List<DiagnosticMedic> getIstoricDiagnosticPacient(UUID pacientId) {
        return repository.findByPacientId(pacientId);
    }
}