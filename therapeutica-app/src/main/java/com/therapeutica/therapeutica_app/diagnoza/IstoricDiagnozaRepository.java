package com.therapeutica.therapeutica_app.diagnoza;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IstoricDiagnozaRepository extends JpaRepository<IstoricDiagnoza, UUID> {

    // Aduce cea mai recentă diagnoză pentru pacient, sortată după data rulării
    Optional<IstoricDiagnoza> findFirstByPacientIdOrderByDataRulareDesc(UUID pacientId);
}