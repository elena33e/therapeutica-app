package com.therapeutica.therapeutica_app.auth;

import com.therapeutica.therapeutica_app.auth.dto.CodVerificareResponse;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareRequest;
import com.therapeutica.therapeutica_app.auth.dto.InregistrareResponse;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrareRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.util.PasswordValidator;
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
    private PasswordEncoder passwordEncoder;

    @Transactional
    public InregistrareResponse inregistreazaUtilizator(InregistrareRequest request) {
        try {
            // Validări de bază
            if (!request.getParola().equals(request.getConfirmaParola())) {
                return new InregistrareResponse(false, "Parolele nu coincid");
            }

            if (!PasswordValidator.isValida(request.getParola())) {
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

           // Creare instanța Utilizator nou
            Utilizatori utilizatorNou = new Utilizatori();
            utilizatorNou.setEmail(cod.getEmailDestinatar());
            utilizatorNou.setRol(cod.getRolDestinatar());
            utilizatorNou.setParola(passwordEncoder.encode(request.getParola()));
            utilizatorNou.setNume(cod.getNumeDestinatar());
            utilizatorNou.setPrenume(cod.getPrenumeDestinatar());
            utilizatoriRepository.save(utilizatorNou);

            // Creare + asociere profil Pacient
            if (cod.getRolDestinatar() == RoleType.PACIENT) {
                Pacienti pacientNou = new Pacienti();
                pacientNou.setUser(utilizatorNou);
                pacientNou.setCnp(cod.getCnpDestinatar());
                pacientNou.setMedic(cod.getGeneratDe().getMedic());
                pacientiRepository.save(pacientNou);
            }

           // Invalidare cod
            cod.setStatus(CodInregistrare.StatusCod.UTILIZAT);
            cod.setAtribuit(utilizatorNou);
            codInregistrareRepository.save(cod);

            return new InregistrareResponse(
                    true,
                    "Cont creat și activat cu succes!",
                    utilizatorNou.getEmail(),
                    utilizatorNou.getRol(),
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


}