package com.therapeutica.therapeutica_app.cod_inregistrare;

import com.therapeutica.therapeutica_app.cod_inregistrare.dto.CodInregistrareDTO;
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
@RequestMapping(value = "/api/codInregistrares", produces = MediaType.APPLICATION_JSON_VALUE)
public class CodInregistrareResource {

    private final CodInregistrareService codInregistrareService;

    public CodInregistrareResource(final CodInregistrareService codInregistrareService) {
        this.codInregistrareService = codInregistrareService;
    }

    @GetMapping
    public ResponseEntity<List<CodInregistrareDTO>> getAllCodInregistrares() {
        return ResponseEntity.ok(codInregistrareService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodInregistrareDTO> getCodInregistrare(
            @PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(codInregistrareService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createCodInregistrare(
            @RequestBody @Valid final CodInregistrareDTO codInregistrareDTO) {
        final UUID createdId = codInregistrareService.create(codInregistrareDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateCodInregistrare(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final CodInregistrareDTO codInregistrareDTO) {
        codInregistrareService.update(id, codInregistrareDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteCodInregistrare(@PathVariable(name = "id") final UUID id) {
        codInregistrareService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
