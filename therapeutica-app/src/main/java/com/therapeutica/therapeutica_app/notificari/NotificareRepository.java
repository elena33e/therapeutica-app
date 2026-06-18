package com.therapeutica.therapeutica_app.notificari;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificareRepository extends JpaRepository<Notificare, Long> {

    // Corectat: se folosește UtilizatorDestinatar_Id pentru a naviga în relația ManyToOne
    List<Notificare> findByUtilizatorDestinatar_IdAndCititaFalseOrderByDataCreareDesc(UUID utilizatorDestinatarId);

    // Utilă pentru istoricul complet de notificări
    List<Notificare> findByUtilizatorDestinatar_IdOrderByDataCreareDesc(UUID utilizatorDestinatarId);

    // Returnează numărul de notificări necitite
    long countByUtilizatorDestinatar_IdAndCititaFalse(UUID utilizatorDestinatarId);
}