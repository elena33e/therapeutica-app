package com.therapeutica.therapeutica_app.medici;

import com.therapeutica.therapeutica_app.events.BeforeDeleteMedici;
import com.therapeutica.therapeutica_app.events.BeforeDeleteUtilizatori;
import com.therapeutica.therapeutica_app.util.CustomCollectors;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.util.ReferencedException;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(rollbackFor = Exception.class)
public class MediciService {

    private final MediciRepository mediciRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final ApplicationEventPublisher publisher;

    public MediciService(final MediciRepository mediciRepository,
            final UtilizatoriRepository utilizatoriRepository,
            final ApplicationEventPublisher publisher) {
        this.mediciRepository = mediciRepository;
        this.utilizatoriRepository = utilizatoriRepository;
        this.publisher = publisher;
    }

    public List<MediciDTO> findAll() {
        final List<Medici> medicis = mediciRepository.findAll(Sort.by("id"));
        return medicis.stream()
                .map(medici -> mapToDTO(medici, new MediciDTO()))
                .toList();
    }

    public MediciDTO get(final UUID id) {
        return mediciRepository.findById(id)
                .map(medici -> mapToDTO(medici, new MediciDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final MediciDTO mediciDTO) {
        final Medici medici = new Medici();
        mapToEntity(mediciDTO, medici);
        return mediciRepository.save(medici).getUserId();
    }

    public void update(final UUID id, final MediciDTO mediciDTO) {
        final Medici medici = mediciRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(mediciDTO, medici);
        mediciRepository.save(medici);
    }

    public void delete(final UUID id) {
        final Medici medici = mediciRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteMedici(id));
        mediciRepository.delete(medici);
    }

    private MediciDTO mapToDTO(final Medici medici, final MediciDTO mediciDTO) {
        mediciDTO.setUserId(medici.getUserId());
        mediciDTO.setSpecializare(medici.getSpecializare());
        mediciDTO.setUserId(medici.getUser() == null ? null : medici.getUser().getId());
        return mediciDTO;
    }

    private Medici mapToEntity(final MediciDTO mediciDTO, final Medici medici) {
        medici.setSpecializare(mediciDTO.getSpecializare());
        final Utilizatori user = mediciDTO.getUserId() == null ? null : utilizatoriRepository.findById(mediciDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("user not found"));
        medici.setUser(user);
        return medici;
    }

    public Map<UUID, UUID> getMediciValues() {
        return mediciRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Medici::getUserId, Medici::getUserId));
    }

    @EventListener(BeforeDeleteUtilizatori.class)
    public void on(final BeforeDeleteUtilizatori event) {
        final ReferencedException referencedException = new ReferencedException();
        final Medici userMedici = mediciRepository.findFirstByUserId(event.getId());
        if (userMedici != null) {
            referencedException.setKey("utilizatori.medici.user.referenced");
            referencedException.addParam(userMedici.getUserId());
            throw referencedException;
        }
    }

}
