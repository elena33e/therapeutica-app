package com.therapeutica.therapeutica_app.chestionare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ChestionareRepository extends JpaRepository<Chestionare, UUID> {

    // Găsește după nume exact
    Optional<Chestionare> findByNume(String nume);


}