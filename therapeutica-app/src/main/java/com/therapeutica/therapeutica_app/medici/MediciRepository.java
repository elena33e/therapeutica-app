package com.therapeutica.therapeutica_app.medici;

import java.util.Optional;
import java.util.UUID;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import org.springframework.data.jpa.repository.JpaRepository;


public interface MediciRepository extends JpaRepository<Medici, UUID> {

    Medici findFirstByUserId(UUID id);
    Optional<Medici> findByUserId(UUID userId);

}
