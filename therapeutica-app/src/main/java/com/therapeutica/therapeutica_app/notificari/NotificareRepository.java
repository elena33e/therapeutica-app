package com.therapeutica.therapeutica_app.notificari;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificareRepository extends JpaRepository<Notificare, Long> {


    // Extrage doar notificările pe care medicul nu le-a văzut încă,
     //si le afiseaza pe cele mai recente
    List<Notificare> findByUtilizatorDestinatarIdAndCititaFalseOrderByDataCreareDesc(UUID utilizatorDestinatarId);

    //Utilă pentru istoricul complet de notificări
    List<Notificare> findByUtilizatorDestinatarIdOrderByDataCreareDesc(UUID utilizatorDestinatarId);

    // Returnează numărul de notificări necitite
    long countByUtilizatorDestinatarIdAndCititaFalse(UUID utilizatorDestinatarId);
}