package com.therapeutica.therapeutica_app.raspunsuri_chestionare;

import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.ChestionareRepository;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.dto.RaspunsChestionarDTO;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.dto.RaspunsChestionarRequestDTO;
import com.therapeutica.therapeutica_app.util.CustomCollectors;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class RaspunsuriChestionareService {

    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final PacientiRepository pacientiRepository;
    private final MediciRepository mediciRepository;
    private final ChestionareRepository chestionareRepository;

    public RaspunsuriChestionareService(
            final RaspunsuriChestionareRepository raspunsuriChestionareRepository,
            final PacientiRepository pacientiRepository,
            final MediciRepository mediciRepository,
            final ChestionareRepository chestionareRepository) {
        this.raspunsuriChestionareRepository = raspunsuriChestionareRepository;
        this.pacientiRepository = pacientiRepository;
        this.mediciRepository = mediciRepository;
        this.chestionareRepository = chestionareRepository;
    }

    public List<RaspunsChestionarDTO> findAll() {
        final List<RaspunsuriChestionare> raspunsuri = raspunsuriChestionareRepository.findAll(Sort.by("completatLa").descending());
        return raspunsuri.stream()
                .map(raspuns -> mapToDTO(raspuns, new RaspunsChestionarDTO()))
                .toList();
    }

    public RaspunsChestionarDTO get(final UUID id) {
        return raspunsuriChestionareRepository.findById(id)
                .map(raspuns -> mapToDTO(raspuns, new RaspunsChestionarDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final RaspunsChestionarRequestDTO raspunsRequestDTO) {
        final RaspunsuriChestionare raspuns = new RaspunsuriChestionare();
        mapToEntity(raspunsRequestDTO, raspuns);
        return raspunsuriChestionareRepository.save(raspuns).getId();
    }

    public void update(final UUID id, final RaspunsChestionarRequestDTO raspunsRequestDTO) {
        final RaspunsuriChestionare raspuns = raspunsuriChestionareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(raspunsRequestDTO, raspuns);
        raspunsuriChestionareRepository.save(raspuns);
    }

    public void delete(final UUID id) {
        final RaspunsuriChestionare raspuns = raspunsuriChestionareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        raspunsuriChestionareRepository.delete(raspuns);
    }

    private RaspunsChestionarDTO mapToDTO(final RaspunsuriChestionare raspuns,
                                          final RaspunsChestionarDTO raspunsDTO) {
        raspunsDTO.setId(raspuns.getId());
        raspunsDTO.setStatus(String.valueOf(raspuns.getStatus()));
        raspunsDTO.setCompletatLa(raspuns.getCompletatLa());
        raspunsDTO.setScorTotalGeneral(raspuns.getScorTotalGeneral());

        // ID-uri din relații
        raspunsDTO.setPacientId(raspuns.getPacient() != null ? raspuns.getPacient().getId() : null);
        raspunsDTO.setMedicId(raspuns.getMedic() != null ? raspuns.getMedic().getUserId() : null); // getUserId() nu getId()
        raspunsDTO.setChestionarId(raspuns.getChestionar() != null ? raspuns.getChestionar().getId() : null);

        // Nume pentru afișare
        if (raspuns.getPacient() != null && raspuns.getPacient().getUser() != null) {
            // Presupun că Utilizatori are getNume() și getPrenume()
            raspunsDTO.setNumePacient(raspuns.getPacient().getUser().getNume() + " " +
                    raspuns.getPacient().getUser().getPrenume());
        }

        if (raspuns.getMedic() != null && raspuns.getMedic().getUser() != null) {
            raspunsDTO.setNumeMedic(raspuns.getMedic().getUser().getNume() + " " +
                    raspuns.getMedic().getUser().getPrenume());
        }

        if (raspuns.getChestionar() != null) {
            raspunsDTO.setNumeChestionar(raspuns.getChestionar().getNume());
        }

        return raspunsDTO;
    }

    private RaspunsuriChestionare mapToEntity(final RaspunsChestionarRequestDTO raspunsRequestDTO,
                                              final RaspunsuriChestionare raspuns) {
        // Setează pacient - caută după ID-ul Pacienti
        if (raspunsRequestDTO.getPacientId() != null) {
            final Pacienti pacient = pacientiRepository.findById(raspunsRequestDTO.getPacientId())
                    .orElseThrow(() -> new NotFoundException("Pacient not found"));
            raspuns.setPacient(pacient);
        }

        // Setează medic - caută după userId (nu id!) pentru Medici
        if (raspunsRequestDTO.getMedicId() != null) {
            final Medici medic = mediciRepository.findById(raspunsRequestDTO.getMedicId())
                    .orElseThrow(() -> new NotFoundException("Medic not found"));
            raspuns.setMedic(medic);
        }

        // Setează chestionar
        if (raspunsRequestDTO.getChestionarId() != null) {
            final Chestionare chestionar = chestionareRepository.findById(raspunsRequestDTO.getChestionarId())
                    .orElseThrow(() -> new NotFoundException("Chestionar not found"));
            raspuns.setChestionar(chestionar);
        }

        raspuns.setStatus(RaspunsuriChestionare.StatusRaspuns.valueOf(raspunsRequestDTO.getStatus()));
        raspuns.setCompletatLa(raspunsRequestDTO.getCompletatLa());
        raspuns.setScorTotalGeneral(raspunsRequestDTO.getScorTotalGeneral());

        return raspuns;
    }

    public Map<UUID, String> getRaspunsuriValues() {
        return raspunsuriChestionareRepository.findAll(Sort.by("completatLa").descending())
                .stream()
                .collect(CustomCollectors.toSortedMap(
                        RaspunsuriChestionare::getId,
                        raspuns -> String.format("Răspuns %s - %s",
                                raspuns.getId().toString().substring(0, 8),
                                raspuns.getStatus())));
    }

    // Metodă utilă: găsește răspunsuri pentru un pacient
    public List<RaspunsChestionarDTO> findByPacientId(UUID pacientId) {
        final List<RaspunsuriChestionare> raspunsuri = raspunsuriChestionareRepository.findAll()
                .stream()
                .filter(r -> r.getPacient() != null && r.getPacient().getId().equals(pacientId))
                .toList();

        return raspunsuri.stream()
                .map(raspuns -> mapToDTO(raspuns, new RaspunsChestionarDTO()))
                .toList();
    }

    // Metodă utilă: găsește răspunsuri pentru un medic
    public List<RaspunsChestionarDTO> findByMedicId(UUID medicId) {
        final List<RaspunsuriChestionare> raspunsuri = raspunsuriChestionareRepository.findAll()
                .stream()
                .filter(r -> r.getMedic() != null && r.getMedic().getUserId().equals(medicId))
                .toList();

        return raspunsuri.stream()
                .map(raspuns -> mapToDTO(raspuns, new RaspunsChestionarDTO()))
                .toList();
    }


    public List<RaspunsChestionarDTO> findByChestionarId(UUID chestionarId) {
        final List<RaspunsuriChestionare> raspunsuri = raspunsuriChestionareRepository.findAll()
                .stream()
                .filter(r -> r.getChestionar() != null && r.getChestionar().getId().equals(chestionarId))
                .toList();

        return raspunsuri.stream()
                .map(raspuns -> mapToDTO(raspuns, new RaspunsChestionarDTO()))
                .toList();
    }
}