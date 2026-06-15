package com.therapeutica.therapeutica_app.resetare_parola;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TokenResetareRepository extends JpaRepository<TokenResetare, UUID> {
    // Spring generează automat implementarea pentru acestă metodă
    Optional<TokenResetare> findByToken(String token);
}