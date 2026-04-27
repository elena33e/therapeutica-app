package com.therapeutica.therapeutica_app.analize_medicale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentMedicalRepository extends JpaRepository<DocumentMedical, UUID> {

    // Găsește toate documentele încărcate pentru un anumit pacient
    List<DocumentMedical> findByPacientId(UUID pacientId);

    // Găsește cel mai recent document al unui pacient
    Optional<DocumentMedical> findFirstByPacientIdOrderByDataIncarcareDesc(UUID pacientId);

    // Verifică dacă un document aparține unui anumit pacient (pentru securitate)
    boolean existsByIdAndPacientId(UUID id, UUID pacientId);

    // Query custom pentru a număra documentele validate
    @Query("SELECT COUNT(d) FROM DocumentMedical d WHERE d.pacientId = :pacientId AND d.status = 'VALIDAT'")
    long countValidatedDocumentsByPacient(@Param("pacientId") UUID pacientId);

    List<DocumentMedical> findByPacientIdOrderByDataIncarcareDesc(UUID pacientId);

    //metoda necesară pentru Worklist-ul medicului
    List<DocumentMedical> findByStatus(DocumentMedical.StatusDocument status);

    List<DocumentMedical> findByPacientIdAndStatusNotOrderByDataIncarcareDesc(UUID pacientId, DocumentMedical.StatusDocument status);
}