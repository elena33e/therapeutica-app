package com.therapeutica.therapeutica_app.utilizatori;

import com.therapeutica.therapeutica_app.events.BeforeDeleteUtilizatori;
import com.therapeutica.therapeutica_app.util.CustomCollectors;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class UtilizatoriService {

    private final UtilizatoriRepository utilizatoriRepository;
    private final ApplicationEventPublisher publisher;

    public UtilizatoriService(
            final UtilizatoriRepository utilizatoriRepository,
            final ApplicationEventPublisher publisher
    ) {
        this.utilizatoriRepository = utilizatoriRepository;
        this.publisher = publisher;
    }

    public List<UtilizatoriDTO> findAll() {
        final List<Utilizatori> utilizatori = utilizatoriRepository.findAll(Sort.by("id"));
        return utilizatori.stream()
                .map(u -> mapToDTO(u, new UtilizatoriDTO()))
                .toList();
    }

    public UtilizatoriDTO get(final UUID id) {
        return utilizatoriRepository.findById(id)
                .map(u -> mapToDTO(u, new UtilizatoriDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final UtilizatoriDTO utilizatoriDTO) {
        final Utilizatori utilizator = new Utilizatori();
        mapToEntity(utilizatoriDTO, utilizator);
        return utilizatoriRepository.save(utilizator).getId();
    }

    public void update(final UUID id, final UtilizatoriDTO utilizatoriDTO) {
        final Utilizatori utilizator = utilizatoriRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(utilizatoriDTO, utilizator);
        utilizatoriRepository.save(utilizator);
    }

    public void delete(final UUID id) {
        final Utilizatori utilizator = utilizatoriRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteUtilizatori(id));
        utilizatoriRepository.delete(utilizator);
    }

    private UtilizatoriDTO mapToDTO(final Utilizatori utilizator, final UtilizatoriDTO dto) {
        dto.setId(utilizator.getId());
        dto.setCreatedAt(utilizator.getCreatedAt());
        dto.setEmail(utilizator.getEmail());
        dto.setNume(utilizator.getNume());
        dto.setPrenume(utilizator.getPrenume());
        dto.setRol(utilizator.getRol());
        return dto;
    }

    private Utilizatori mapToEntity(final UtilizatoriDTO dto, final Utilizatori utilizator) {
        utilizator.setCreatedAt(dto.getCreatedAt());
        utilizator.setEmail(dto.getEmail());
        utilizator.setNume(dto.getNume());
        utilizator.setPrenume(dto.getPrenume());
        utilizator.setRol(dto.getRol());
        return utilizator;
    }

    public Map<UUID, String> getUtilizatoriValues() {
        return utilizatoriRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Utilizatori::getId, u -> u.getRol().toString()));
    }

    @EventListener(BeforeDeleteUtilizatori.class)
    public void on(final BeforeDeleteUtilizatori event) {
        // aici NU trebuie șters nimic automat — Medici și Pacienti verifică FK-urile
    }
}
