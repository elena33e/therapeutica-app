package com.therapeutica.therapeutica_app.atribuire_chestionar;

import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.ChestionareRepository;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.notificari.NotificareListenerService;
import com.therapeutica.therapeutica_app.notificari.events.NotificareEvent;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class AtribuireChestionarService {

    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final PacientiRepository pacientiRepository;
    private final MediciRepository mediciRepository;
    private final ChestionareRepository chestionareRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Atribuie unul sau mai multe chestionare unui pacient
     */
    @Transactional(rollbackFor = Exception.class)
    public List<UUID> atribuiChestionarePacientului(AtribuireChestionarRequestDTO requestDTO) {
        log.info("Inițiere atribuire chestionare pentru pacient: {} de către medic: {}",
                requestDTO.getPacientId(), requestDTO.getMedicId());

        // 1. Validare rapidă input
        if (requestDTO.getChestionareIds() == null || requestDTO.getChestionareIds().isEmpty()) {
            throw new IllegalArgumentException("Trebuie selectat cel puțin un chestionar.");
        }

        // 2. Rezolvare entități (Medicul și Pacientul)
        final Medici medic = mediciRepository.findById(requestDTO.getMedicId())
                .orElseThrow(() -> new NotFoundException("Medicul nu a fost găsit."));

        // Fallback logic: Caută pacientul după ID-ul primit (care poate fi ID-ul entității sau UserID)
        final Pacienti pacient = pacientiRepository.findById(requestDTO.getPacientId())
                .or(() -> pacientiRepository.findByUserId(requestDTO.getPacientId()))
                .orElseThrow(() -> new NotFoundException("Pacientul nu a fost găsit."));

        // 3. Verificare securitate
        if (!estePacientAlMedicului(pacient, medic)) {
            log.warn("Tentativă neautorizată: Medicul {} a încercat să atribuie chestionare pacientului {}",
                    medic.getUserId(), pacient.getId());
            throw new SecurityException("Pacientul nu este asociat acestui medic.");
        }

        // 4. Procesare atribuiri
        List<UUID> raspunsuriCreateIds = new ArrayList<>();

        for (UUID chestionarId : requestDTO.getChestionareIds()) {
            // Evităm duplicatele active aruncând excepția așteptată de Controller
            if (existaChestionarNecompletat(pacient.getId(), chestionarId)) {
                log.warn("Tentativă de duplicare: Chestionarul {} este deja atribuit și necompletat pentru pacientul {}", chestionarId, pacient.getId());
                throw new IllegalStateException("Pacientul are deja o instanță activă și necompletată pentru unul dintre chestionarele selectate.");
            }

            chestionareRepository.findById(chestionarId).ifPresent(chestionar -> {
                RaspunsuriChestionare raspuns = new RaspunsuriChestionare();
                raspuns.setPacient(pacient);
                raspuns.setMedic(medic);
                raspuns.setChestionar(chestionar);
                raspuns.setStatus(RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);

                RaspunsuriChestionare saved = raspunsuriChestionareRepository.save(raspuns);
                raspunsuriCreateIds.add(saved.getId());
            });
        }

        // Notificare Pacient (doar dacă s-au creat atribuiri noi)
        if (!raspunsuriCreateIds.isEmpty()) {
            triggerNotificarePacient(pacient, raspunsuriCreateIds.size());
        }

        log.info("Succes: S-au atribuit {} chestionare noi pentru pacientul {}",
                raspunsuriCreateIds.size(), pacient.getId());

        return raspunsuriCreateIds;
    }

    //Metodă notificare
    private void triggerNotificarePacient(Pacienti pacient, int numarChestionare) {
        try {
            if (pacient.getUser() != null) {
                UUID userId = pacient.getUser().getId();
                String link = "/chestionare/pacient/" + userId + "/disponibile";
                String titlu = numarChestionare > 1 ? "Chestionare noi" : "Chestionar nou";
                String mesaj = "Medicul tău ți-a atribuit " + numarChestionare + " chestionare noi pentru completare.";

                eventPublisher.publishEvent(new NotificareEvent(userId, titlu, mesaj, link));
                log.debug("Eveniment de notificare trimis către UserID: {}", userId);
            }
        } catch (Exception e) {
            log.error("Eroare silențioasă la trimiterea notificării: {}", e.getMessage());
        }
    }

    /**
     * Verifică dacă pacientul este asociat medicului
     */
    private boolean estePacientAlMedicului(Pacienti pacient, Medici medic) {
        // Verifică dacă pacientul are medicul setat sau dacă medicul apare în lista de pacienți
        return pacient.getMedic() != null && pacient.getMedic().getUserId().equals(medic.getUserId());
    }

    /**
     * Verifică dacă există deja un chestionar necompletat pentru același pacient
     */
    private boolean existaChestionarNecompletat(UUID pacientId, UUID chestionarId) {
        return raspunsuriChestionareRepository
                .existsByPacientIdAndChestionarIdAndStatus(pacientId, chestionarId, RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);
    }

    /**
     * Obține chestionarele disponibile pentru atribuire (care nu sunt deja atribuite și necompletate)
     */
    public List<Chestionare> getChestionareDisponibilePentruPacient(UUID pacientId) {
        log.info("Getting available questionnaires for patient: {}", pacientId);

        // Obținem toate chestionarele din sistem
        List<Chestionare> toateChestionarele = chestionareRepository.findAll();
        log.info("   - Total questionnaires in system: {}", toateChestionarele.size());

        // Filtrăm: un chestionar este disponibil DOAR dacă NU are deja o instanță NECOMPLETATĂ activă
        List<Chestionare> disponibile = toateChestionarele.stream()
                .filter(chestionar -> !existaChestionarNecompletat(pacientId, chestionar.getId()))
                .toList();

        log.info("   - Available for assignment: {} questionnaires", disponibile.size());

        return disponibile;
    }

    /**
     * Verifică dacă pacientul este asociat medicului (public method pentru API)
     */
    public boolean estePacientAsociatMedicului(UUID pacientId, UUID medicId) {
        final Pacienti pacient = pacientiRepository.findById(pacientId)
                .orElseThrow(() -> new NotFoundException("Pacient not found"));

        final Medici medic = mediciRepository.findById(medicId)
                .orElseThrow(() -> new NotFoundException("Medic not found"));

        return estePacientAlMedicului(pacient, medic);
    }

    /**
     * Obține istoricul chestionarelor pentru un pacient
     */
    public List<RaspunsuriChestionare> getIstoricChestionarePacient(UUID pacientId) {
        return raspunsuriChestionareRepository.findByPacientIdWithRelations(pacientId);
    }

}