package com.therapeutica.therapeutica_app.utilizatori;

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
@RequestMapping(value = "/api/utilizatoris", produces = MediaType.APPLICATION_JSON_VALUE)
public class UtilizatoriResource {

    private final UtilizatoriService utilizatoriService;

    public UtilizatoriResource(final UtilizatoriService utilizatoriService) {
        this.utilizatoriService = utilizatoriService;
    }

    @GetMapping
    public ResponseEntity<List<UtilizatoriDTO>> getAllUtilizatoris() {
        return ResponseEntity.ok(utilizatoriService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilizatoriDTO> getUtilizatori(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(utilizatoriService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createUtilizatori(
            @RequestBody @Valid final UtilizatoriDTO utilizatoriDTO) {
        final UUID createdId = utilizatoriService.create(utilizatoriDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateUtilizatori(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final UtilizatoriDTO utilizatoriDTO) {
        utilizatoriService.update(id, utilizatoriDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteUtilizatori(@PathVariable(name = "id") final UUID id) {
        utilizatoriService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
