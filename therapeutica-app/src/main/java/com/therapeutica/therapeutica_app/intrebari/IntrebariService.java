package com.therapeutica.therapeutica_app.intrebari;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionareRepository;
import com.therapeutica.therapeutica_app.intrebari.dto.CreateIntrebareRequest;
import com.therapeutica.therapeutica_app.intrebari.dto.IntrebareDTO;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class IntrebariService {

    private final IntrebariRepository intrebariRepository;
    private final CategoriiChestionareRepository categoriiChestionareRepository;
    private final ObjectMapper objectMapper;

    public IntrebareDTO createIntrebare(CreateIntrebareRequest request) {
        CategoriiChestionare categorie = categoriiChestionareRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new NotFoundException("Categoria nu a fost găsită"));

        Intrebare intrebare = new Intrebare();
        intrebare.setCategorie(categorie);
        intrebare.setTextIntrebare(request.getTextIntrebare());
        intrebare.setTipIntrebare(request.getTipIntrebare());
        intrebare.setOrdine(request.getOrdine());
        intrebare.setObligatorie(request.getObligatorie());

        // Procesează opțiunile pentru MULTIPLE_CHOICE
        if (request.getOptiuni() != null && !request.getOptiuni().isEmpty()) {
            try {
                String optiuniJson = objectMapper.writeValueAsString(request.getOptiuni());
                intrebare.setOptiuniJson(optiuniJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Eroare la serializarea opțiunilor", e);
            }
        }

        Intrebare saved = intrebariRepository.save(intrebare);
        return IntrebareDTO.fromEntity(saved);
    }

    public List<IntrebareDTO> getIntrebariByCategorie(UUID categorieId) {
        List<Intrebare> intrebari = intrebariRepository.findByCategorieIdOrderByOrdine(categorieId);
        return intrebari.stream()
                .map(IntrebareDTO::fromEntity)
                .toList();
    }

    public List<IntrebareDTO> getIntrebariByChestionar(UUID chestionarId) {
        List<Intrebare> intrebari = intrebariRepository.findByChestionarId(chestionarId);
        return intrebari.stream()
                .map(IntrebareDTO::fromEntity)
                .toList();
    }

    public IntrebareDTO getIntrebareById(UUID id) {
        Intrebare intrebare = intrebariRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Întrebarea nu a fost găsită"));
        return IntrebareDTO.fromEntity(intrebare);
    }

    public void deleteIntrebare(UUID id) {
        if (!intrebariRepository.existsById(id)) {
            throw new NotFoundException("Întrebarea nu a fost găsită");
        }
        intrebariRepository.deleteById(id);
    }

    public IntrebareDTO updateIntrebare(UUID id, CreateIntrebareRequest request) {
        Intrebare intrebare = intrebariRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Întrebarea nu a fost găsită"));

        if (request.getCategorieId() != null && !request.getCategorieId().equals(intrebare.getCategorie().getId())) {
            CategoriiChestionare categorie = categoriiChestionareRepository.findById(request.getCategorieId())
                    .orElseThrow(() -> new NotFoundException("Categoria nu a fost găsită"));
            intrebare.setCategorie(categorie);
        }

        if (request.getTextIntrebare() != null) {
            intrebare.setTextIntrebare(request.getTextIntrebare());
        }

        if (request.getTipIntrebare() != null) {
            intrebare.setTipIntrebare(request.getTipIntrebare());
        }

        if (request.getOrdine() != null) {
            intrebare.setOrdine(request.getOrdine());
        }

        if (request.getObligatorie() != null) {
            intrebare.setObligatorie(request.getObligatorie());
        }

        if (request.getOptiuni() != null) {
            try {
                String optiuniJson = objectMapper.writeValueAsString(request.getOptiuni());
                intrebare.setOptiuniJson(optiuniJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Eroare la serializarea opțiunilor", e);
            }
        }

        Intrebare updated = intrebariRepository.save(intrebare);
        return IntrebareDTO.fromEntity(updated);
    }
}