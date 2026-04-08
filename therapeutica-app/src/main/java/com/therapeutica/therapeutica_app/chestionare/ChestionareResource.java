package com.therapeutica.therapeutica_app.chestionare;



import com.therapeutica.therapeutica_app.chestionare.dto.ChestionarDTO;
import com.therapeutica.therapeutica_app.chestionare.dto.ChestionarRequestDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller pentru gestionarea entității Chestionare.
 */
@RestController
@RequestMapping(value = "/api/chestionare", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChestionareResource {

    private final ChestionareService chestionareService;

    public ChestionareResource(final ChestionareService chestionareService) {
        this.chestionareService = chestionareService;
    }

    @GetMapping
    public ResponseEntity<List<ChestionarDTO>> getAllChestionare() {
        return ResponseEntity.ok(chestionareService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChestionarDTO> getChestionar(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(chestionareService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createChestionar(@RequestBody @Valid final ChestionarRequestDTO chestionarRequestDTO) {
        final UUID createdId = chestionareService.create(chestionarRequestDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateChestionar(@PathVariable(name = "id") final UUID id,
                                                 @RequestBody @Valid final ChestionarRequestDTO chestionarRequestDTO) {
        chestionareService.update(id, chestionarRequestDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteChestionar(@PathVariable(name = "id") final UUID id) {
        chestionareService.delete(id);
        return ResponseEntity.noContent().build();
    }
}