package com.therapeutica.therapeutica_app.categorii_chestionare;

import com.therapeutica.therapeutica_app.categorii_chestionare.dto.CategoriiChestionarRequestDTO;
import com.therapeutica.therapeutica_app.categorii_chestionare.dto.CategoriiChestionareDTO;
import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.ChestionareRepository;
import com.therapeutica.therapeutica_app.util.CustomCollectors;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class CategoriiChestionareService {

    private final CategoriiChestionareRepository categoriiChestionareRepository;
    private final ChestionareRepository chestionareRepository;

    public CategoriiChestionareService(
            final CategoriiChestionareRepository categoriiChestionareRepository,
            final ChestionareRepository chestionareRepository) {
        this.categoriiChestionareRepository = categoriiChestionareRepository;
        this.chestionareRepository = chestionareRepository;
    }

    public List<CategoriiChestionareDTO> findAll() {
        final List<CategoriiChestionare> categorii = categoriiChestionareRepository.findAll(Sort.by("nume"));
        return categorii.stream()
                .map(categorie -> mapToDTO(categorie, new CategoriiChestionareDTO()))
                .toList();
    }

    public List<CategoriiChestionareDTO> findAllByChestionarId(final UUID chestionarId) {
        final List<CategoriiChestionare> categorii = categoriiChestionareRepository.findAll(Sort.by("nume"));
        return categorii.stream()
                .filter(categorie -> categorie.getChestionar() != null &&
                        categorie.getChestionar().getId().equals(chestionarId))
                .map(categorie -> mapToDTO(categorie, new CategoriiChestionareDTO()))
                .toList();
    }

    public CategoriiChestionareDTO get(final UUID id) {
        return categoriiChestionareRepository.findById(id)
                .map(categorie -> mapToDTO(categorie, new CategoriiChestionareDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final CategoriiChestionarRequestDTO categorieRequestDTO) {
        final CategoriiChestionare categorie = new CategoriiChestionare();
        mapToEntity(categorieRequestDTO, categorie);
        return categoriiChestionareRepository.save(categorie).getId();
    }

    public void update(final UUID id, final CategoriiChestionarRequestDTO categorieRequestDTO) {
        final CategoriiChestionare categorie = categoriiChestionareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(categorieRequestDTO, categorie);
        categoriiChestionareRepository.save(categorie);
    }

    public void delete(final UUID id) {
        final CategoriiChestionare categorie = categoriiChestionareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        categoriiChestionareRepository.delete(categorie);
    }

    private CategoriiChestionareDTO mapToDTO(final CategoriiChestionare categorie,
                                            final CategoriiChestionareDTO categorieDTO) {
        categorieDTO.setId(categorie.getId());
        categorieDTO.setNume(categorie.getNume());
        categorieDTO.setChestionarId(categorie.getChestionar() != null ?
                categorie.getChestionar().getId() : null);
        categorieDTO.setNumeChestionar(categorie.getChestionar() != null ?
                categorie.getChestionar().getNume() : null);
        return categorieDTO;
    }

    private CategoriiChestionare mapToEntity(final CategoriiChestionarRequestDTO categorieRequestDTO,
                                             final CategoriiChestionare categorie) {
        categorie.setNume(categorieRequestDTO.getNume());

        final Chestionare chestionar = chestionareRepository
                .findById(categorieRequestDTO.getChestionarId())
                .orElseThrow(() -> new NotFoundException("Chestionar not found"));
        categorie.setChestionar(chestionar);

        return categorie;
    }

    public Map<UUID, String> getCategoriiValues() {
        return categoriiChestionareRepository.findAll(Sort.by("nume"))
                .stream()
                .collect(CustomCollectors.toSortedMap(
                        CategoriiChestionare::getId,
                        CategoriiChestionare::getNume));
    }

    public Map<UUID, String> getCategoriiValuesByChestionar(final UUID chestionarId) {
        return categoriiChestionareRepository.findAll(Sort.by("nume"))
                .stream()
                .filter(categorie -> categorie.getChestionar() != null &&
                        categorie.getChestionar().getId().equals(chestionarId))
                .collect(CustomCollectors.toSortedMap(
                        CategoriiChestionare::getId,
                        CategoriiChestionare::getNume));
    }
}