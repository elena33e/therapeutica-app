package com.therapeutica.therapeutica_app.chestionare;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionareService;
import com.therapeutica.therapeutica_app.categorii_chestionare.dto.CategoriiChestionareDTO;
import com.therapeutica.therapeutica_app.categorii_chestionare.dto.CategoriiChestionarRequestDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller pentru gestionarea entității CategoriiChestionare.
 */
@RestController
@RequestMapping(value = "/api/categorii-chestionare", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoriiChestionareResource {

    private final CategoriiChestionareService categoriiChestionareService;

    public CategoriiChestionareResource(final CategoriiChestionareService categoriiChestionareService) {
        this.categoriiChestionareService = categoriiChestionareService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriiChestionareDTO>> getAllCategoriiChestionare() {
        return ResponseEntity.ok(categoriiChestionareService.findAll());
    }

    @GetMapping("/chestionar/{chestionarId}")
    public ResponseEntity<List<CategoriiChestionareDTO>> getCategoriiByChestionar(
            @PathVariable(name = "chestionarId") final UUID chestionarId) {
        return ResponseEntity.ok(categoriiChestionareService.findAllByChestionarId(chestionarId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriiChestionareDTO> getCategorieChestionar(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(categoriiChestionareService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createCategorieChestionar(
            @RequestBody @Valid final CategoriiChestionarRequestDTO categorieChestionarRequestDTO) {
        final UUID createdId = categoriiChestionareService.create(categorieChestionarRequestDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateCategorieChestionar(
            @PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final CategoriiChestionarRequestDTO categorieChestionarRequestDTO) {
        categoriiChestionareService.update(id, categorieChestionarRequestDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteCategorieChestionar(@PathVariable(name = "id") final UUID id) {
        categoriiChestionareService.delete(id);
        return ResponseEntity.noContent().build();
    }
}