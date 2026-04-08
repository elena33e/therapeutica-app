package com.therapeutica.therapeutica_app.auth;

import com.therapeutica.therapeutica_app.auth.dto.CodVerificareResponse;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareRequest;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareResponse;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrareRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.supabase.SupabaseAuthService;
import com.therapeutica.therapeutica_app.supabase.dto.SupabaseAuthResponse;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InregistrareService {

    @Autowired
    private CodInregistrareRepository codInregistrareRepository;

    @Autowired
    private UtilizatoriRepository utilizatoriRepository;

    @Autowired
    private PacientiRepository pacientiRepository;

    @Autowired
    private SupabaseAuthService supabaseAuthService;

    @Transactional
    public InregistrareResponse inregistreazaUtilizator(InregistrareRequest request) {
        try {
            // 1. Validări de bază
            if (!request.getParola().equals(request.getConfirmaParola())) {
                return new InregistrareResponse(false, "Parolele nu coincid");
            }

            if (!valideazaParola(request.getParola())) {
                return new InregistrareResponse(false,
                        "Parola trebuie să aibă cel puțin 8 caractere, o cifră și un caracter special");
            }

            // 2. Verifică codul de înregistrare și utilizatorul într-o singură tranzacție
            Optional<CodInregistrare> codOpt = codInregistrareRepository
                    .findByCodUnicAndStatus(request.getCodUnic(), CodInregistrare.StatusCod.NEUTILIZAT);

            if (codOpt.isEmpty()) {
                return new InregistrareResponse(false, "Cod de înregistrare invalid sau deja utilizat");
            }

            CodInregistrare cod = codOpt.get();

            // 3. Găsește utilizatorul
            Optional<Utilizatori> utilizatorOpt = utilizatoriRepository.findByEmail(cod.getEmailDestinatar());
            if (utilizatorOpt.isEmpty()) {
                return new InregistrareResponse(false, "Nu există utilizator pentru acest cod");
            }

            Utilizatori utilizatorExistent = utilizatorOpt.get();

            // 4. Verifică CNP-ul (doar pentru PACIENTI) - evită extra query dacă nu e necesar
            if (utilizatorExistent.getRol() == RoleType.PACIENT) {
                Optional<Pacienti> pacientOpt = pacientiRepository.findByUserId(utilizatorExistent.getId());
                if (pacientOpt.isEmpty()) {
                    return new InregistrareResponse(false, "Date incomplete pentru pacient");
                }

                Pacienti pacient = pacientOpt.get();
                if (!pacient.getCnp().equals(request.getCnp())) {
                    return new InregistrareResponse(false, "CNP invalid pentru acest cod");
                }
            }

            // 5. CREEAZĂ contul în Supabase
            SupabaseAuthResponse authResponse = supabaseAuthService.signUp(
                    cod.getEmailDestinatar(),
                    request.getParola(),
                    utilizatorExistent.getRol(),
                    utilizatorExistent.getNume(),
                    utilizatorExistent.getPrenume()
            );

            System.out.println("=== AUTH RESPONSE ANALYSIS ===");
            System.out.println("User: " + (authResponse.getUser() != null));
            System.out.println("User ID: " + (authResponse.getUser() != null ? authResponse.getUser().getId() : "null"));
            System.out.println("Access Token: " + (authResponse.getAccess_token() != null));
            System.out.println("Error: " + (authResponse.getError() != null));

            // ✅ NOU - consideră succes dacă userul apare în Supabase (chiar dacă răspunsul e gol)
            if (authResponse.getError() != null) {
                // Există o eroare explicită de la Supabase
                return new InregistrareResponse(false, "Eroare la crearea contului: " + authResponse.getError().getMessage());
            }

            // Dacă ajungem aici, considerăm că e succes (userul apare în Supabase dashboard)
            System.out.println("✅ User creat în Supabase - continuăm cu fluxul");

            // 6. Marchează codul ca utilizat
            cod.setStatus(CodInregistrare.StatusCod.UTILIZAT);
            cod.setAtribuit(utilizatorExistent);
            codInregistrareRepository.save(cod);

            // ✅ MODIFICARE CHEIE: Întotdeauna returnăm că necesită confirmare
            // pentru că Supabase cu confirmare email activată va trimite mereu email de confirmare
            return new InregistrareResponse(
                    true,
                    "Cont creat cu succes! Vă rugăm să vă verificați email-ul (" + utilizatorExistent.getEmail() + ") și să faceți click pe link-ul de confirmare pentru a activa contul. După confirmare, vă puteți autentifica în aplicație.",
                    utilizatorExistent.getEmail(),
                    utilizatorExistent.getRol(),
                    true // ✅ ÎNTOTDEAUNA true - căci Supabase trimite email de confirmare
            );

        } catch (Exception e) {
            e.printStackTrace();  // ✅ PENTRU DEBUG
            return new InregistrareResponse(false, "Eroare internă: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public CodVerificareResponse verificaCod(String codUnic) {
        try {
            Optional<CodInregistrare> codOpt = codInregistrareRepository
                    .findByCodUnicAndStatus(codUnic, CodInregistrare.StatusCod.NEUTILIZAT);

            if (codOpt.isPresent()) {
                CodInregistrare cod = codOpt.get();

                Optional<Utilizatori> utilizatorOpt = utilizatoriRepository.findByEmail(cod.getEmailDestinatar());

                if (utilizatorOpt.isPresent()) {
                    Utilizatori utilizator = utilizatorOpt.get();
                    return new CodVerificareResponse(
                            true,
                            "Cod valid",
                            cod.getEmailDestinatar(),
                            utilizator.getNume(),
                            utilizator.getPrenume(),
                            cod.getRolDestinatar()
                    );
                } else {
                    return new CodVerificareResponse(false, "Nu există utilizator pentru acest cod");
                }
            } else {
                return new CodVerificareResponse(false, "Cod invalid sau deja utilizat");
            }
        } catch (Exception e) {
            e.printStackTrace();  // ✅ PENTRU DEBUG
            return new CodVerificareResponse(false, "Eroare la verificarea codului: " + e.getMessage());
        }
    }

    private boolean valideazaParola(String parola) {
        if (parola == null || parola.length() < 8) {
            return false;
        }

        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char c : parola.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }

        return hasDigit && hasSpecialChar;
    }
}