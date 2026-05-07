// src/main/java/com/therapeutica/therapeutica_app/cod_inregistrare/CodInregistrareController.java
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
@CrossOrigin(origins = "*") // Rectificare: Permite cererile din front-end
public class CodInregistrareController {

    private static final Logger logger = LoggerFactory.getLogger(CodInregistrareController.class);
    private final CodInregistrareService codInregistrareService;

    public CodInregistrareController(CodInregistrareService codInregistrareService) {
        this.codInregistrareService = codInregistrareService;
    }

    @GetMapping
    public ResponseEntity<List<CodInregistrareDTO>> getAllCoduri() {
        logger.info("GET /api/coduri-inregistrare - All codes");
        return ResponseEntity.ok(codInregistrareService.findAll());
    }

    @GetMapping("/medic/{medicId}")
    public ResponseEntity<List<CodInregistrareDTO>> getCoduriByMedic(@PathVariable UUID medicId) {
        logger.info("GET /api/coduri-inregistrare/medic/{}", medicId);
        List<CodInregistrareDTO> coduri = codInregistrareService.getCoduriByMedic(medicId);
        return ResponseEntity.ok(coduri);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodInregistrareDTO> getCodById(@PathVariable UUID id) {
        logger.info("GET /api/coduri-inregistrare/{}", id);
        CodInregistrareDTO cod = codInregistrareService.get(id);
        return ResponseEntity.ok(cod);
    }

    // ========== POST ENDPOINTS ==========

    @PostMapping("/generare")
    public ResponseEntity<GenerareCodResponse> generareCod(
            @Valid @RequestBody GenerareCodRequest request) {

        logger.info("=== API POST /api/coduri-inregistrare/generare ===");
        logger.info("Medic ID: {}, Email: {}, Rol: {}, CNP: {}",
                request.getMedicId(), request.getEmailDestinatar(),
                request.getRolDestinatar(), request.getCnpDestinatar());

        try {
            GenerareCodResponse response = codInregistrareService.generareCodInregistrare(request);

            if (response.getMesaj() != null && response.getMesaj().contains("Eroare")) {
                logger.error("Error in service: {}", response.getMesaj());
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Cod generat cu succes: {}", response.getCodUnic());

            // Rectificare: Returnăm 200 OK în loc de 201 CREATED pentru stabilitate în JS
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Exception in controller: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenerareCodResponse("Eroare server: " + e.getMessage()));
        }
    }

    // ========== PUT ENDPOINTS ==========

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCod(
            @PathVariable UUID id,
            @Valid @RequestBody CodInregistrareDTO codDTO) {
        logger.info("PUT /api/coduri-inregistrare/{}", id);
        codInregistrareService.update(id, codDTO);
        return ResponseEntity.ok().build();
    }

    // ========== DELETE ENDPOINTS ==========

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCod(@PathVariable UUID id) {
        logger.info("DELETE /api/coduri-inregistrare/{}", id);
        codInregistrareService.delete(id);
        return ResponseEntity.noContent().build();
    }
}