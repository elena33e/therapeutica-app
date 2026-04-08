package com.therapeutica.therapeutica_app.raspunsuri_chestionare;

import com.therapeutica.therapeutica_app.raspunsuri_chestionare.dto.RaspunsChestionarDTO;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.dto.RaspunsChestionarRequestDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/raspunsuri-chestionare", produces = MediaType.APPLICATION_JSON_VALUE)
public class RaspunsuriChestionareResource {

    private final RaspunsuriChestionareService raspunsuriChestionareService;

    public RaspunsuriChestionareResource(final RaspunsuriChestionareService raspunsuriChestionareService) {
        this.raspunsuriChestionareService = raspunsuriChestionareService;
    }

    @GetMapping
    public ResponseEntity<List<RaspunsChestionarDTO>> getAllRaspunsuriChestionare() {
        return ResponseEntity.ok(raspunsuriChestionareService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RaspunsChestionarDTO> getRaspunsChestionar(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(raspunsuriChestionareService.get(id));
    }

    @GetMapping("/pacient/{pacientId}")
    public ResponseEntity<List<RaspunsChestionarDTO>> getRaspunsuriByPacient(
            @PathVariable(name = "pacientId") final UUID pacientId) {
        return ResponseEntity.ok(raspunsuriChestionareService.findByPacientId(pacientId));
    }

    @GetMapping("/medic/{medicId}")
    public ResponseEntity<List<RaspunsChestionarDTO>> getRaspunsuriByMedic(
            @PathVariable(name = "medicId") final UUID medicId) {
        return ResponseEntity.ok(raspunsuriChestionareService.findByMedicId(medicId));
    }

    @GetMapping("/chestionar/{chestionarId}")
    public ResponseEntity<List<RaspunsChestionarDTO>> getRaspunsuriByChestionar(
            @PathVariable(name = "chestionarId") final UUID chestionarId) {
        return ResponseEntity.ok(raspunsuriChestionareService.findByChestionarId(chestionarId));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createRaspunsChestionar(
            @RequestBody @Valid final RaspunsChestionarRequestDTO raspunsChestionarRequestDTO) {
        final UUID createdId = raspunsuriChestionareService.create(raspunsChestionarRequestDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateRaspunsChestionar(
            @PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final RaspunsChestionarRequestDTO raspunsChestionarRequestDTO) {
        raspunsuriChestionareService.update(id, raspunsChestionarRequestDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteRaspunsChestionar(@PathVariable(name = "id") final UUID id) {
        raspunsuriChestionareService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
