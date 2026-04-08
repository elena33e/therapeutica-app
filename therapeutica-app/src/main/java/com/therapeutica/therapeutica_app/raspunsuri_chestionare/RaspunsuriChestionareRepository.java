package com.therapeutica.therapeutica_app.raspunsuri_chestionare;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RaspunsuriChestionareRepository extends JpaRepository<RaspunsuriChestionare, UUID> {

    // Pentru AtribuireChestionarService.getChestionareDisponibilePentruPacient()
    @Query("SELECT r FROM RaspunsuriChestionare r WHERE r.pacient.id = :pacientId AND r.status = 'NECOMPLETAT'")
    List<RaspunsuriChestionare> findByPacientIdAndStatusNecompletat(@Param("pacientId") UUID pacientId);

    @Query("SELECT COUNT(r) > 0 FROM RaspunsuriChestionare r WHERE " +
            "r.pacient.id = :pacientId AND " +
            "r.chestionar.id = :chestionarId AND " +
            "r.status = 'NECOMPLETAT'")
    boolean existsByPacientIdAndChestionarIdAndStatusNecompletat(
            @Param("pacientId") UUID pacientId,
            @Param("chestionarId") UUID chestionarId);

    @Query("SELECT r FROM RaspunsuriChestionare r WHERE r.pacient.id = :pacientId AND r.status = :status")
    List<RaspunsuriChestionare> findByPacientIdAndStatus(
            @Param("pacientId") UUID pacientId,
            @Param("status") RaspunsuriChestionare.StatusRaspuns status);

    // Pentru frontend - istoric chestionare pacient
    @Query("SELECT r FROM RaspunsuriChestionare r WHERE r.pacient.id = :pacientId ORDER BY r.completatLa DESC NULLS FIRST")
    List<RaspunsuriChestionare> findByPacientIdOrderByCompletatLa(@Param("pacientId") UUID pacientId);

    // Pentru frontend - chestionarele unui medic
    @Query("SELECT r FROM RaspunsuriChestionare r WHERE r.medic.userId = :medicId")
    List<RaspunsuriChestionare> findByMedicId(@Param("medicId") UUID medicId);

    @Query("SELECT rc FROM RaspunsuriChestionare rc " +
            "LEFT JOIN FETCH rc.chestionar c " +
            "LEFT JOIN FETCH rc.pacient p " +
            "LEFT JOIN FETCH rc.medic m " +
            "WHERE rc.pacient.id = :pacientId " +
            "ORDER BY rc.completatLa DESC")
    List<RaspunsuriChestionare> findByPacientIdWithRelations(@Param("pacientId") UUID pacientId);

    @Query("SELECT rc FROM RaspunsuriChestionare rc " +
            "LEFT JOIN FETCH rc.chestionar c " +           // Chestionar
            "LEFT JOIN FETCH rc.medic m " +                // Medic
            "LEFT JOIN FETCH m.user mu " +                 // User-ul medicului (cu nume, prenume)
            "LEFT JOIN FETCH rc.pacient p " +              // Pacient
            "LEFT JOIN FETCH p.user pu " +                 // User-ul pacientului
            "WHERE rc.pacient.id = :pacientId " +
            "AND rc.status = :status " +
            "ORDER BY rc.completatLa DESC NULLS LAST")
    List<RaspunsuriChestionare> findByPacientIdAndStatusFullRelations(
            @Param("pacientId") UUID pacientId,
            @Param("status") RaspunsuriChestionare.StatusRaspuns status);

    @Query("SELECT rc FROM RaspunsuriChestionare rc " +
            "LEFT JOIN FETCH rc.chestionar " +
            "LEFT JOIN FETCH rc.pacient p " +
            "LEFT JOIN FETCH p.user " +  // ← ASTA E CHEIA!
            "WHERE rc.id = :id")
    Optional<RaspunsuriChestionare> findByIdForCompletare(@Param("id") UUID id);

    @Query("SELECT rc FROM RaspunsuriChestionare rc " +
            "LEFT JOIN FETCH rc.chestionar " +
            "WHERE rc.id = :id")
    Optional<RaspunsuriChestionare> findByIdWithChestionar(@Param("id") UUID id);

    @Query("SELECT c.nume FROM RaspunsuriChestionare rc " +
            "JOIN rc.chestionar c WHERE rc.id = :id")
    String findNumeChestionarById(@Param("id") UUID id);

    @Query("SELECT p.user.id FROM RaspunsuriChestionare rc " +
            "JOIN rc.pacient p WHERE rc.id = :raspunsChestionarId")
    UUID findUserIdByRaspunsChestionarId(@Param("raspunsChestionarId") UUID raspunsChestionarId);


}