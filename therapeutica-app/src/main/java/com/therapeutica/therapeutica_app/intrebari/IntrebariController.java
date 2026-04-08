package com.therapeutica.therapeutica_app.intrebari;

import com.therapeutica.therapeutica_app.intrebari.dto.CreateIntrebareRequest;
import com.therapeutica.therapeutica_app.intrebari.dto.IntrebareDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/intrebari")
@RequiredArgsConstructor
public class IntrebariController {

    private final IntrebariService intrebariService;

    @PostMapping
    public ResponseEntity<IntrebareDTO> createIntrebare(@Valid @RequestBody CreateIntrebareRequest request) {
        IntrebareDTO created = intrebariService.createIntrebare(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<List<IntrebareDTO>> getIntrebariByCategorie(@PathVariable UUID categorieId) {
        List<IntrebareDTO> intrebari = intrebariService.getIntrebariByCategorie(categorieId);
        return ResponseEntity.ok(intrebari);
    }

    @GetMapping("/chestionar/{chestionarId}")
    public ResponseEntity<List<IntrebareDTO>> getIntrebariByChestionar(@PathVariable UUID chestionarId) {
        List<IntrebareDTO> intrebari = intrebariService.getIntrebariByChestionar(chestionarId);
        return ResponseEntity.ok(intrebari);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntrebareDTO> getIntrebare(@PathVariable UUID id) {
        IntrebareDTO intrebare = intrebariService.getIntrebareById(id);
        return ResponseEntity.ok(intrebare);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IntrebareDTO> updateIntrebare(
            @PathVariable UUID id,
            @Valid @RequestBody CreateIntrebareRequest request) {
        IntrebareDTO updated = intrebariService.updateIntrebare(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntrebare(@PathVariable UUID id) {
        intrebariService.deleteIntrebare(id);
        return ResponseEntity.noContent().build();
    }
}