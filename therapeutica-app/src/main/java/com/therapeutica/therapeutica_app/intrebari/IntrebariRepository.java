package com.therapeutica.therapeutica_app.intrebari;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntrebariRepository extends JpaRepository<Intrebare, UUID> {

    // Găsește întrebările pentru o categorie
    List<Intrebare> findByCategorieIdOrderByOrdine(UUID categorieId);

    // Găsește întrebările pentru un chestionar (prin categorii)
    @Query("SELECT i FROM Intrebare i " +
            "JOIN i.categorie c " +
            "WHERE c.chestionar.id = :chestionarId " +
            "ORDER BY c.ordine, i.ordine")
    List<Intrebare> findByChestionarId(@Param("chestionarId") UUID chestionarId);

    List<Intrebare> findByCategorieId(UUID categorieId);;

    // Găsește întrebările pentru o categorie cu un anumit tip
    List<Intrebare> findByCategorieIdAndTipIntrebare(UUID categorieId, Intrebare.TipIntrebare tipIntrebare);

    // Numără întrebările pentru un chestionar
    @Query("SELECT COUNT(i) FROM Intrebare i " +
            "JOIN i.categorie c " +
            "WHERE c.chestionar.id = :chestionarId")
    long countByChestionarId(@Param("chestionarId") UUID chestionarId);
}