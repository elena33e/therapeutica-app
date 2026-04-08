package com.therapeutica.therapeutica_app.raport_chestionar;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RezultatScoringRepository extends JpaRepository<RezultatScoring, UUID> {

    @Query("SELECT rs FROM RezultatScoring rs WHERE rs.raspunsChestionar.id = :raspunsChestionarId")
    Optional<RezultatScoring> findByRaspunsChestionarId(@Param("raspunsChestionarId") UUID raspunsChestionarId);

    boolean existsByRaspunsChestionarId(UUID raspunsChestionarId);

    @Query("DELETE FROM RezultatScoring rs WHERE rs.raspunsChestionar.id = :raspunsChestionarId")
    void deleteByRaspunsChestionarId(@Param("raspunsChestionarId") UUID raspunsChestionarId);
}
