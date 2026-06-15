package com.therapeutica.therapeutica_app.diagnostic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
public interface DiagnosticMedicRepository extends JpaRepository<DiagnosticMedic, UUID> {
    List<DiagnosticMedic> findByPacientId(UUID pacientId);

    @Query("SELECT d FROM DiagnosticMedic d " +
            "JOIN FETCH d.pacient p " +
            "JOIN FETCH p.medic m " +
            "JOIN FETCH m.user u " +
            "WHERE d.pacient.id = :pacientId")
    List<DiagnosticMedic> findByPacientIdWithFullDetails(UUID pacientId);
}