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

    List<DocumentMedical> findByPacient_Id(UUID pacientId);


    Optional<DocumentMedical> findFirstByPacient_IdOrderByDataIncarcareDesc(UUID pacientId);

    boolean existsByIdAndPacient_Id(UUID id, UUID pacientId);

    @Query("SELECT COUNT(d) FROM DocumentMedical d WHERE d.pacient.id = :pacientId AND d.status = 'VALIDAT'")
    long countValidatedDocumentsByPacient(@Param("pacientId") UUID pacientId);


    List<DocumentMedical> findByPacient_IdOrderByDataIncarcareDesc(UUID pacientId);

    List<DocumentMedical> findByStatus(DocumentMedical.StatusDocument status);


    List<DocumentMedical> findByPacient_IdAndStatusNotOrderByDataIncarcareDesc(UUID pacientId, DocumentMedical.StatusDocument status);
}