package com.therapeutica.therapeutica_app.categorii_chestionare;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoriiChestionareRepository extends JpaRepository<CategoriiChestionare, UUID> {


    //@Query("SELECT c FROM CategoriiChestionare c WHERE c.chestionar.id = :chestionarId")
    List<CategoriiChestionare> findByChestionarId(@Param("chestionarId") UUID chestionarId);

    // Alte metode utile:
    List<CategoriiChestionare> findByChestionarIdOrderByNume(UUID chestionarId);

    @Query("SELECT c FROM CategoriiChestionare c " +
            "LEFT JOIN FETCH c.intrebari " +
            "WHERE c.chestionar.id = :chestionarId")
    List<CategoriiChestionare> findByChestionarIdWithIntrebari(@Param("chestionarId") UUID chestionarId);

    boolean existsByChestionarIdAndNume(UUID chestionarId, String nume);

    long countByChestionarId(UUID chestionarId);
}
