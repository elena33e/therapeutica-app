package com.therapeutica.therapeutica_app.cod_inregistrare;

import com.therapeutica.therapeutica_app.cod_inregistrare.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coduri-inregistrare")
@CrossOrigin(origins = "*")
public class CodInregistrareController {

    private static final Logger logger = LoggerFactory.getLogger(CodInregistrareController.class);
    private final CodInregistrareService codInregistrareService;

    public CodInregistrareController(CodInregistrareService codInregistrareService) {
        this.codInregistrareService = codInregistrareService;
    }

    @GetMapping
    public ResponseEntity<List<CodInregistrareDTO>> getAllCoduri() {
        return ResponseEntity.ok(codInregistrareService.findAll());
    }

    @GetMapping("/medic/{medicId}")
    public ResponseEntity<List<CodInregistrareDTO>> getCoduriByMedic(@PathVariable UUID medicId) {
        return ResponseEntity.ok(codInregistrareService.getCoduriByMedic(medicId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodInregistrareDTO> getCodById(@PathVariable UUID id) {
        return ResponseEntity.ok(codInregistrareService.get(id));
    }

    @PostMapping("/generare")
    public ResponseEntity<GenerareCodResponse> generareCod(@Valid @RequestBody GenerareCodRequest request) {
        logger.info("=== POST /api/coduri-inregistrare/generare | Medic: {}, Email: {} ===", request.getMedicId(), request.getEmailDestinatar());

        try {
            GenerareCodResponse response = codInregistrareService.generareCodInregistrare(request);

            if (response.getMesaj() != null && response.getMesaj().startsWith("Eroare")) {
                logger.warn("Eroare de business la generare cod: {}", response.getMesaj());
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Excepție critică în controller: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenerareCodResponse("Eroare server: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCod(@PathVariable UUID id, @Valid @RequestBody CodInregistrareDTO codDTO) {
        codInregistrareService.update(id, codDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCod(@PathVariable UUID id) {
        codInregistrareService.delete(id);
        return ResponseEntity.noContent().build();
    }
}