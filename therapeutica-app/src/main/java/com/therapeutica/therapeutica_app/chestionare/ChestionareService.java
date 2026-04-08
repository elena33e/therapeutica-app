package com.therapeutica.therapeutica_app.chestionare;


import com.therapeutica.therapeutica_app.chestionare.dto.ChestionarDTO;
import com.therapeutica.therapeutica_app.chestionare.dto.ChestionarRequestDTO;
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
public class ChestionareService {

    private final ChestionareRepository chestionareRepository;

    public ChestionareService(final ChestionareRepository chestionareRepository) {
        this.chestionareRepository = chestionareRepository;
    }

    public List<ChestionarDTO> findAll() {
        final List<Chestionare> chestionare = chestionareRepository.findAll(Sort.by("creatLa").descending());
        return chestionare.stream()
                .map(chestionar -> mapToDTO(chestionar, new ChestionarDTO()))
                .toList();
    }

    public ChestionarDTO get(final UUID id) {
        return chestionareRepository.findById(id)
                .map(chestionar -> mapToDTO(chestionar, new ChestionarDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final ChestionarRequestDTO chestionarRequestDTO) {
        final Chestionare chestionar = new Chestionare();
        mapToEntity(chestionarRequestDTO, chestionar);
        return chestionareRepository.save(chestionar).getId();
    }

    public void update(final UUID id, final ChestionarRequestDTO chestionarRequestDTO) {
        final Chestionare chestionar = chestionareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(chestionarRequestDTO, chestionar);
        chestionareRepository.save(chestionar);
    }

    public void delete(final UUID id) {
        final Chestionare chestionar = chestionareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        chestionareRepository.delete(chestionar);
    }

    private ChestionarDTO mapToDTO(final Chestionare chestionar, final ChestionarDTO chestionarDTO) {
        chestionarDTO.setId(chestionar.getId());
        chestionarDTO.setNume(chestionar.getNume());
        chestionarDTO.setDescriere(chestionar.getDescriere());
        chestionarDTO.setInstructiuni(chestionar.getInstructiuni());
        chestionarDTO.setCreatLa(chestionar.getCreatLa());
        // Număr de categorii dacă ai nevoie
        chestionarDTO.setNumarCategorii(chestionar.getCategoriiChestionare() != null
                ? chestionar.getCategoriiChestionare().size()
                : 0);
        return chestionarDTO;
    }

    private Chestionare mapToEntity(final ChestionarRequestDTO chestionarRequestDTO, final Chestionare chestionar) {
        chestionar.setNume(chestionarRequestDTO.getNume());
        chestionar.setDescriere(chestionarRequestDTO.getDescriere());
        chestionar.setInstructiuni(chestionarRequestDTO.getInstructiuni());
        return chestionar;
    }

    public Map<UUID, String> getChestionareValues() {
        return chestionareRepository.findAll(Sort.by("nume"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Chestionare::getId, Chestionare::getNume));
    }
}