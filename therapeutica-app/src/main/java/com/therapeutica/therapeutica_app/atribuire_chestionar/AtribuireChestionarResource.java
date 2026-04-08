package com.therapeutica.therapeutica_app.atribuire_chestionar;

import com.therapeutica.therapeutica_app.atribuire_chestionar.AtribuireChestionarRequestDTO;
import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/atribuire-chestionare", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Atribuire Chestionare", description = "API pentru atribuirea chestionarelor pacienților")
public class AtribuireChestionarResource {

    private final AtribuireChestionarService atribuireChestionarService;

    public AtribuireChestionarResource(final AtribuireChestionarService atribuireChestionarService) {
        this.atribuireChestionarService = atribuireChestionarService;
    }

    @PostMapping
    @Operation(
            summary = "Atribuie chestionare unui pacient",
            description = "Medicul atribuie unul sau mai multe chestionare unui pacient asociat. " +
                    "Se trimite notificare prin email către pacient."
    )
    @ApiResponse(responseCode = "201", description = "Chestionare atribuite cu succes")
    @ApiResponse(responseCode = "400", description = "Date invalide sau lista de chestionare goală")
    @ApiResponse(responseCode = "404", description = "Pacient, medic sau chestionar negăsit")
    @ApiResponse(responseCode = "403", description = "Medicul nu are drepturi asupra pacientului")
    public ResponseEntity<List<UUID>> atribuiChestionare(
            @RequestBody @Valid final AtribuireChestionarRequestDTO requestDTO) {

        List<UUID> raspunsuriIds = atribuireChestionarService.atribuiChestionarePacientului(requestDTO);
        return new ResponseEntity<>(raspunsuriIds, HttpStatus.CREATED);
    }

    @GetMapping("/disponibile/{pacientId}")
    @Operation(
            summary = "Obține chestionarele disponibile pentru un pacient",
            description = "Returnează chestionarele care pot fi atribuite unui pacient " +
                    "(care nu sunt deja atribuite și în starea NECOMPLETAT)"
    )
    public ResponseEntity<List<Chestionare>> getChestionareDisponibile(
            @PathVariable(name = "pacientId") final UUID pacientId) {

        List<Chestionare> chestionare = atribuireChestionarService.getChestionareDisponibilePentruPacient(pacientId);
        return ResponseEntity.ok(chestionare);
    }

    @GetMapping("/pacient/{pacientId}/medic/{medicId}")
    @Operation(
            summary = "Verifică dacă pacientul este asociat medicului",
            description = "Endpoint pentru validarea drepturilor înainte de atribuire"
    )
    public ResponseEntity<Boolean> verificaAsocierePacientMedic(
            @PathVariable(name = "pacientId") final UUID pacientId,
            @PathVariable(name = "medicId") final UUID medicId) {

        boolean esteAsociat = atribuireChestionarService.estePacientAsociatMedicului(pacientId, medicId);
        return ResponseEntity.ok(esteAsociat);
    }

    @GetMapping("/istoric/{pacientId}")
    @Operation(
            summary = "Obține istoricul chestionarelor unui pacient",
            description = "Returnează toate chestionarele atribuite pacientului"
    )
    public ResponseEntity<List<RaspunsuriChestionare>> getIstoricChestionare(
            @PathVariable(name = "pacientId") final UUID pacientId) {

        // Folosește service-ul, nu repository direct!
        List<RaspunsuriChestionare> istoric = atribuireChestionarService.getIstoricChestionarePacient(pacientId);
        return ResponseEntity.ok(istoric);
    }
}