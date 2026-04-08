package com.therapeutica.therapeutica_app.pacienti;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface PacientiRepository extends JpaRepository<Pacienti, UUID> {

    Optional<Pacienti> findFirstByUserId(UUID userId);
    // Caută pacient după user_id (nu utilizator_id)
    Optional<Pacienti> findByUserId(UUID userId);

    // Metoda pentru a obține pacienții cu user încărcat
    @Query("SELECT p FROM Pacienti p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH p.medic m " +
            "WHERE p.medic.userId = :medicId")
    List<Pacienti> findPacientiByMedicIdWithUser(@Param("medicId") UUID medicId);

    // Metoda pentru a obține un pacient specific cu user încărcat
    @Query("SELECT p FROM Pacienti p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH p.medic m " +
            "LEFT JOIN FETCH m.user mu " + // ✅ Încarcă și user-ul medicului
            "WHERE p.id = :pacientId")
    Optional<Pacienti> findByIdWithUser(@Param("pacientId") UUID pacientId);

    // Adaugă asta în PacientiRepository:
    @Query("SELECT p FROM Pacienti p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH p.medic m " +
            "LEFT JOIN FETCH m.user mu " +
            "WHERE u.id = :userId")
    Optional<Pacienti> findByUserIdWithFullData(@Param("userId") UUID userId);
}
