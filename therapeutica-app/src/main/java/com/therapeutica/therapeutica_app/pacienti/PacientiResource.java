package com.therapeutica.therapeutica_app.pacienti;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller pentru gestionarea entității Pacienti.
 */
@RestController
@RequestMapping(value = "/api/pacientis", produces = MediaType.APPLICATION_JSON_VALUE)
public class PacientiResource {

    private final PacientiService pacientiService;

    public PacientiResource(final PacientiService pacientiService) {
        this.pacientiService = pacientiService;
    }

    @GetMapping
    public ResponseEntity<List<PacientiDTO>> getAllPacientis() {
        return ResponseEntity.ok(pacientiService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacientiDTO> getPacienti(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(pacientiService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createPacienti(@RequestBody @Valid final PacientiDTO pacientiDTO) {
        final UUID createdId = pacientiService.create(pacientiDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updatePacienti(@PathVariable(name = "id") final UUID id,
                                               @RequestBody @Valid final PacientiDTO pacientiDTO) {
        pacientiService.update(id, pacientiDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deletePacienti(@PathVariable(name = "id") final UUID id) {
        pacientiService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
