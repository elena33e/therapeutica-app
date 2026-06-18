package com.therapeutica.therapeutica_app.notificari;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificari")
@RequiredArgsConstructor
public class NotificariApiController {

    private final NotificareRepository notificareRepository;

    @GetMapping("/necitite/{medicId}")
    public List<Notificare> getNotificari(@PathVariable UUID medicId) {

        return notificareRepository.findByUtilizatorDestinatar_IdAndCititaFalseOrderByDataCreareDesc(medicId);
    }

    @GetMapping("/necitite-count/{medicId}")
    public long getNumarNotificariNecitite(@PathVariable UUID medicId) {

        return notificareRepository.countByUtilizatorDestinatar_IdAndCititaFalse(medicId);
    }

    @PostMapping("/marcheaza-citita/{id}")
    public void marcheazaCitita(@PathVariable Long id) {
        notificareRepository.findById(id).ifPresent(n -> {
            n.setCitita(true);
            notificareRepository.save(n);
        });
    }

    @GetMapping("/acceseaza/{id}")
    public RedirectView acceseazaNotificareSiRedirectioneaza(@PathVariable Long id) {
        Optional<Notificare> notificareOpt = notificareRepository.findById(id);

        if (notificareOpt.isPresent()) {
            Notificare notificare = notificareOpt.get();
            if (!notificare.isCitita()) {
                notificare.setCitita(true);
                notificareRepository.save(notificare);
            }
            return new RedirectView(notificare.getLinkActiune());
        }

        // Fallback: Îl trimitem la un dashboard sigur, nu pe root ("/")
        return new RedirectView("/medic/notificari");
    }
}