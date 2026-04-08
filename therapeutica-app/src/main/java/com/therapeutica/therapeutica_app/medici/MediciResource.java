package com.therapeutica.therapeutica_app.medici;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/medicis", produces = MediaType.APPLICATION_JSON_VALUE)
public class MediciResource {

    private final MediciService mediciService;

    public MediciResource(final MediciService mediciService) {
        this.mediciService = mediciService;
    }

    @GetMapping
    public ResponseEntity<List<MediciDTO>> getAllMedicis() {
        return ResponseEntity.ok(mediciService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediciDTO> getMedici(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(mediciService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createMedici(@RequestBody @Valid final MediciDTO mediciDTO) {
        final UUID createdId = mediciService.create(mediciDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateMedici(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final MediciDTO mediciDTO) {
        mediciService.update(id, mediciDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteMedici(@PathVariable(name = "id") final UUID id) {
        mediciService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
