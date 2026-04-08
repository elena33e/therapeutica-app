package com.therapeutica.therapeutica_app.raspunsuri_intrebari;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RaspunsuriIntrebariRepository extends JpaRepository<RaspunsuriIntrebari, UUID> {

    List<RaspunsuriIntrebari> findByRaspunsChestionarId(UUID raspunsChestionarId);


    Optional<RaspunsuriIntrebari> findByRaspunsChestionarIdAndIntrebareId(
            UUID raspunsChestionarId, UUID intrebareId);


    List<RaspunsuriIntrebari> findByRaspunsChestionarIdAndCategorieId(
            UUID raspunsChestionarId, UUID categorieId);


    boolean existsByRaspunsChestionarIdAndIntrebareId(UUID raspunsChestionarId, UUID intrebareId);


    long countByRaspunsChestionarId(UUID raspunsChestionarId);


    @Query("SELECT AVG(ri.scor) FROM RaspunsuriIntrebari ri " +
            "WHERE ri.raspunsChestionar.id = :raspunsChestionarId " +
            "AND ri.categorie.id = :categorieId " +
            "AND ri.scor IS NOT NULL")
    Double findAverageScoreByCategorie(@Param("raspunsChestionarId") UUID raspunsChestionarId,
                                       @Param("categorieId") UUID categorieId);

    @Query("SELECT ri FROM RaspunsuriIntrebari ri " +
            "JOIN FETCH ri.intrebare i " +
            "JOIN FETCH ri.categorie c " +
            "WHERE ri.raspunsChestionar.id = :id " +
            "ORDER BY c.ordine ASC, i.ordine ASC")
    List<RaspunsuriIntrebari> findByRaspunsChestionarIdWithDetails(@Param("id") UUID id);
}
