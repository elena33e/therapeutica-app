package com.therapeutica.therapeutica_app.notificari.ui; // Ajustează pachetul conform structurii tale

import com.therapeutica.therapeutica_app.notificari.Notificare;
import com.therapeutica.therapeutica_app.notificari.NotificareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificariController {

    private final NotificareRepository notificareRepository;

    @GetMapping("/medic/notificari/{medicId}")
    public String afiseazaInboxNotificari(@PathVariable UUID medicId, Model model) {
        log.info("Afișare inbox notificări pentru medicul: {}", medicId);

        // 1. Extragem toate notificările, sortate de la cele mai noi la cele mai vechi
        List<Notificare> notificari = notificareRepository.findByUtilizatorDestinatarIdOrderByDataCreareDesc(medicId);

        // 2. Marcăm notificările necitite ca fiind "citite" (deoarece medicul le vizualizează acum)
        boolean areNotificariNoi = false;
        for (Notificare notif : notificari) {
            if (!notif.isCitita()) {
                notif.setCitita(true);
                areNotificariNoi = true;
            }
        }

        // Dacă am modificat cel puțin o notificare, salvăm noile statusuri în baza de date
        if (areNotificariNoi) {
            notificareRepository.saveAll(notificari);
            log.info("Notificările necitite au fost marcate ca citite pentru medicul: {}", medicId);
        }

        // 3. Trimitem datele către pagina web
        model.addAttribute("notificari", notificari);
        model.addAttribute("medicId", medicId);

        // 4. Returnăm vizualizarea
        return "medic/notificari";
    }
}