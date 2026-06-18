package com.therapeutica.therapeutica_app.notificari.ui;

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
    public String afiseazaInboxMedic(@PathVariable UUID medicId, Model model) {
        return proceseazaInbox(medicId, model, "medic/notificari", "medicId");
    }

    @GetMapping("/pacient/notificari/{pacientId}")
    public String afiseazaInboxPacient(@PathVariable UUID pacientId, Model model) {
        return proceseazaInbox(pacientId, model, "pacient/notificari", "pacientId");
    }

    private String proceseazaInbox(UUID userId, Model model, String viewName, String idLabel) {
        log.info("Încărcare inbox pentru utilizatorul: {}", userId);

        List<Notificare> notificari = notificareRepository.findByUtilizatorDestinatar_IdOrderByDataCreareDesc(userId);
        model.addAttribute("notificari", notificari);
        model.addAttribute(idLabel, userId);

        return viewName;
    }
}