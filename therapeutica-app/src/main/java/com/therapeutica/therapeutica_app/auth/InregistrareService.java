package com.therapeutica.therapeutica_app.auth;

import com.therapeutica.therapeutica_app.auth.dto.CodVerificareResponse;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareRequest;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareResponse;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrareRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder; // Adaugă acest câmp în InregistrareService

    @Transactional
    public InregistrareResponse inregistreazaUtilizator(InregistrareRequest request) {
        try {
            // Validări de bază
            if (!request.getParola().equals(request.getConfirmaParola())) {
                return new InregistrareResponse(false, "Parolele nu coincid");
            }

            if (!valideazaParola(request.getParola())) {
                return new InregistrareResponse(false,
                        "Parola trebuie să aibă cel puțin 8 caractere, o cifră și un caracter special");
            }

            // Verificare cod inregistrare
            Optional<CodInregistrare> codOpt = codInregistrareRepository
                    .findByCodUnicAndStatus(request.getCodUnic(), CodInregistrare.StatusCod.NEUTILIZAT);

            if (codOpt.isEmpty()) {
                return new InregistrareResponse(false, "Cod de înregistrare invalid sau deja utilizat");
            }

            CodInregistrare cod = codOpt.get();

            // Găsește utilizatorul asociat email-ului din cod
            Optional<Utilizatori> utilizatorOpt = utilizatoriRepository.findByEmail(cod.getEmailDestinatar());
            if (utilizatorOpt.isEmpty()) {
                return new InregistrareResponse(false, "Nu există cont asociat acestui cod");
            }

            Utilizatori utilizator = utilizatorOpt.get();

            // Verificare CNP pacient
            if (utilizator.getRol() == RoleType.PACIENT) {
                Optional<Pacienti> pacientOpt = pacientiRepository.findByUserId(utilizator.getId());
                if (pacientOpt.isEmpty()) {
                    return new InregistrareResponse(false, "Date incomplete pentru pacient");
                }

                Pacienti pacient = pacientOpt.get();
                if (pacient.getCnp() != null && !pacient.getCnp().equals(request.getCnp())) {
                    return new InregistrareResponse(false, "CNP invalid");
                }
            }

            // Criptarea parolei și actualizarea utilizatorului local
            utilizator.setParola(passwordEncoder.encode(request.getParola()));
            utilizatoriRepository.save(utilizator);

            // Marchează codul ca utilizat
            cod.setStatus(CodInregistrare.StatusCod.UTILIZAT);
            cod.setAtribuit(utilizator);
            codInregistrareRepository.save(cod);

            return new InregistrareResponse(
                    true,
                    "Cont activat cu succes! Vă puteți autentifica acum folosind email-ul și parola setată.",
                    utilizator.getEmail(),
                    utilizator.getRol(),
                    false
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new InregistrareResponse(false, "Eroare internă la procesarea înregistrării: " + e.getMessage());
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
            e.printStackTrace();
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