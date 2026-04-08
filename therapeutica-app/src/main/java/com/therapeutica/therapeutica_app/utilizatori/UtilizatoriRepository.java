package com.therapeutica.therapeutica_app.utilizatori;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilizatoriRepository extends JpaRepository<Utilizatori, UUID> {

    /**
     * Găsește toți utilizatorii care sunt pacienți pentru un anumit medic.
     * @param medicId ID-ul medicului
     * @return lista de Utilizatori care sunt pacienți ai medicului
     */
    @Query("SELECT p.user FROM Pacienti p WHERE p.medic.userId = :medicId")
    List<Utilizatori> findAllByMedicId(@Param("medicId") UUID medicId);
    Optional<Utilizatori> findByEmail(String email);
    boolean existsByEmail(String email);
}
