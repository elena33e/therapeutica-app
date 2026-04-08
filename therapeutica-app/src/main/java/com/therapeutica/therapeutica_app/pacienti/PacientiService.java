package com.therapeutica.therapeutica_app.pacienti;

import com.therapeutica.therapeutica_app.events.BeforeDeletePacienti;
import com.therapeutica.therapeutica_app.events.BeforeDeleteUtilizatori;
import com.therapeutica.therapeutica_app.util.CustomCollectors;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.util.ReferencedException;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class PacientiService {

    private final PacientiRepository pacientiRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final MediciRepository mediciRepository;
    private final ApplicationEventPublisher publisher;

    public PacientiService(
            final PacientiRepository pacientiRepository,
            final UtilizatoriRepository utilizatoriRepository,
            final MediciRepository mediciRepository,
            final ApplicationEventPublisher publisher
    ) {
        this.pacientiRepository = pacientiRepository;
        this.utilizatoriRepository = utilizatoriRepository;
        this.mediciRepository = mediciRepository;
        this.publisher = publisher;
    }

    public List<PacientiDTO> findAll() {
        final List<Pacienti> pacienti = pacientiRepository.findAll(Sort.by("id"));
        return pacienti.stream()
                .map(pacient -> mapToDTO(pacient, new PacientiDTO()))
                .toList();
    }

    public PacientiDTO get(final UUID id) {
        return pacientiRepository.findById(id)
                .map(pacient -> mapToDTO(pacient, new PacientiDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final PacientiDTO pacientiDTO) {
        final Pacienti pacient = new Pacienti();
        mapToEntity(pacientiDTO, pacient);
        return pacientiRepository.save(pacient).getId();
    }

    public void update(final UUID id, final PacientiDTO pacientiDTO) {
        final Pacienti pacient = pacientiRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(pacientiDTO, pacient);
        pacientiRepository.save(pacient);
    }

    public void delete(final UUID id) {
        final Pacienti pacient = pacientiRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeletePacienti(id));
        pacientiRepository.delete(pacient);
    }

    private PacientiDTO mapToDTO(final Pacienti pacient, final PacientiDTO pacientiDTO) {
        pacientiDTO.setId(pacient.getId());
        pacientiDTO.setUser(pacient.getUser() == null ? null : pacient.getUser().getId());
        pacientiDTO.setMedic(pacient.getMedic() == null ? null : pacient.getMedic().getUserId());
        return pacientiDTO;
    }

    private Pacienti mapToEntity(final PacientiDTO pacientiDTO, final Pacienti pacient) {
        final Utilizatori user = pacientiDTO.getUser() == null ? null :
                utilizatoriRepository.findById(pacientiDTO.getUser())
                        .orElseThrow(() -> new NotFoundException("user not found"));
        pacient.setUser(user);

        final Medici medic = pacientiDTO.getMedic() == null ? null :
                mediciRepository.findById(pacientiDTO.getMedic())
                        .orElseThrow(() -> new NotFoundException("medic not found"));
        pacient.setMedic(medic);

        return pacient;
    }

    public Map<UUID, UUID> getPacientiValues() {
        return pacientiRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Pacienti::getId, Pacienti::getId));
    }

    @EventListener(BeforeDeleteUtilizatori.class)
    public void on(final BeforeDeleteUtilizatori event) {
        final ReferencedException referencedException = new ReferencedException();
        final Optional<Pacienti> userPacient = pacientiRepository.findFirstByUserId(event.getId());
        if (userPacient != null) {
            referencedException.setKey("utilizatori.pacienti.user.referenced");
            referencedException.addParam(userPacient.get());
            throw referencedException;
        }
    }
}
