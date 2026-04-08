package com.therapeutica.therapeutica_app.cod_inregistrare;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodInregistrareRepository extends JpaRepository<CodInregistrare, UUID> {

    CodInregistrare findFirstByGeneratDeId(UUID id);

    CodInregistrare findFirstByAtribuitId(UUID id);

    Optional<CodInregistrare> findByCodUnicAndStatus(String codUnic, CodInregistrare.StatusCod status);


    // Verifică dacă un cod există
    boolean existsByCodUnic(String codUnic);

    // Găsește cod după valoare (pentru verificare la înregistrare)
    Optional<CodInregistrare> findByCodUnic(String codUnic);

    // Toate codurile generate de un medic
    @Query("SELECT c FROM CodInregistrare c WHERE c.generatDe.id = :medicId ORDER BY c.createdAt DESC")
    List<CodInregistrare> findByGeneratDeId(@Param("medicId") UUID medicId);

    // Coduri neutilizate pentru un email (pentru validare)
    @Query("SELECT c FROM CodInregistrare c WHERE c.emailDestinatar = :email AND c.status = 'NEUTILIZAT'")
    List<CodInregistrare> findNeutilizatByEmail(@Param("email") String email);

    // Coduri după medic și status
    List<CodInregistrare> findByGeneratDeIdAndStatus(UUID medicId, CodInregistrare.StatusCod status);
}