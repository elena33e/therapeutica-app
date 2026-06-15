package com.therapeutica.therapeutica_app.auth;

import com.therapeutica.therapeutica_app.auth.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private InregistrareService inregistrareService;

    @PostMapping("/inregistrare")
    @Transactional
    public ResponseEntity<InregistrareResponse> inregistrare(@RequestBody InregistrareRequest request) {
        try {
            InregistrareResponse response = inregistrareService.inregistreazaUtilizator(request);
            HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new InregistrareResponse(false, "Eroare server: " + e.getMessage()));
        }
    }

    // Metoda pentru verificarea codului rămâne de asemenea activă.
    @GetMapping("/verifica-cod/{codUnic}")
    @Transactional(readOnly = true)
    public ResponseEntity<CodVerificareResponse> verificaCod(@PathVariable String codUnic) {
        CodVerificareResponse response = inregistrareService.verificaCod(codUnic);
        return ResponseEntity.ok(response);
    }

}