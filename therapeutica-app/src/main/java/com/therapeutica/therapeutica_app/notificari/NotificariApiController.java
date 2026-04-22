package com.therapeutica.therapeutica_app.notificari;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificari")
@RequiredArgsConstructor
public class NotificariApiController {

    private final NotificareRepository notificareRepository;

    @GetMapping("/necitite/{medicId}")
    public List<Notificare> getNotificari(@PathVariable UUID medicId) {
        return notificareRepository.findByUtilizatorDestinatarIdAndCititaFalseOrderByDataCreareDesc(medicId);
    }

    @PostMapping("/marcheaza-citita/{id}")
    public void marcheazaCitita(@PathVariable Long id) {
        notificareRepository.findById(id).ifPresent(n -> {
            n.setCitita(true);
            notificareRepository.save(n);
        });
    }
}