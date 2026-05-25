package com.therapeutica.therapeutica_app.diagnostic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
public interface DiagnosticMedicRepository extends JpaRepository<DiagnosticMedic, UUID> {
    List<DiagnosticMedic> findByPacientId(UUID pacientId);
}