// src/main/java/com/therapeutica/therapeutica_app/cod_inregistrare/CodInregistrareController.java
package com.therapeutica.therapeutica_app.cod_inregistrare;

import com.therapeutica.therapeutica_app.cod_inregistrare.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coduri-inregistrare")
public class CodInregistrareController {

    private final CodInregistrareService codInregistrareService;

    public CodInregistrareController(CodInregistrareService codInregistrareService) {
        this.codInregistrareService = codInregistrareService;
    }

    @GetMapping
    public ResponseEntity<List<CodInregistrareDTO>> getAllCoduri() {
        System.out.println("GET /api/coduri-inregistrare - All codes");
        return ResponseEntity.ok(codInregistrareService.findAll());
    }

    @GetMapping("/medic/{medicId}")
    public ResponseEntity<List<CodInregistrareDTO>> getCoduriByMedic(@PathVariable UUID medicId) {
        System.out.println("GET /api/coduri-inregistrare/medic/" + medicId);
        List<CodInregistrareDTO> coduri = codInregistrareService.getCoduriByMedic(medicId);
        return ResponseEntity.ok(coduri);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodInregistrareDTO> getCodById(@PathVariable UUID id) {
        System.out.println("GET /api/coduri-inregistrare/" + id);
        CodInregistrareDTO cod = codInregistrareService.get(id);
        return ResponseEntity.ok(cod);
    }

    // ========== POST ENDPOINTS ==========

    // POST pentru generare cod
    @PostMapping("/generare")
    public ResponseEntity<GenerareCodResponse> generareCod(
            @Valid @RequestBody GenerareCodRequest request) {

        System.out.println("=== API POST /api/coduri-inregistrare/generare ===");
        System.out.println("Medic ID: " + request.getMedicId());
        System.out.println("Email destinatar: " + request.getEmailDestinatar());
        System.out.println("Rol destinatar: " + request.getRolDestinatar());
        System.out.println("CNP destinatar: " + request.getCnpDestinatar());

        try {
            GenerareCodResponse response = codInregistrareService.generareCodInregistrare(request);

            if (response.getMesaj() != null && response.getMesaj().contains("Eroare")) {
                System.out.println("❌ Error in service: " + response.getMesaj());
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("✅ Cod generat cu succes: " + response.getCodUnic());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.out.println("❌ Exception in controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenerareCodResponse("Eroare server: " + e.getMessage()));
        }
    }

    // ========== PUT ENDPOINTS ==========

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCod(
            @PathVariable UUID id,
            @Valid @RequestBody CodInregistrareDTO codDTO) {
        System.out.println("PUT /api/coduri-inregistrare/" + id);
        codInregistrareService.update(id, codDTO);
        return ResponseEntity.ok().build();
    }

    // ========== DELETE ENDPOINTS ==========

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCod(@PathVariable UUID id) {
        System.out.println("DELETE /api/coduri-inregistrare/" + id);
        codInregistrareService.delete(id);
        return ResponseEntity.noContent().build();
    }
}