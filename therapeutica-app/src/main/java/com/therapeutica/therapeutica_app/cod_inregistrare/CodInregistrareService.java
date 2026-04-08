package com.therapeutica.therapeutica_app.cod_inregistrare;

import com.therapeutica.therapeutica_app.cod_inregistrare.dto.CodInregistrareDTO;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodRequest;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodResponse;
import com.therapeutica.therapeutica_app.events.BeforeDeleteUtilizatori;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.util.ReferencedException;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class CodInregistrareService {

    private final CodInregistrareRepository codInregistrareRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final PacientiRepository pacientiRepository; // ← ADAUGĂ ASTA

    public CodInregistrareService(final CodInregistrareRepository codInregistrareRepository,
                                  final UtilizatoriRepository utilizatoriRepository,
                                  final PacientiRepository pacientiRepository) {
        this.codInregistrareRepository = codInregistrareRepository;
        this.utilizatoriRepository = utilizatoriRepository;
        this.pacientiRepository = pacientiRepository;
    }

    public List<CodInregistrareDTO> findAll() {
        final List<CodInregistrare> codInregistrares = codInregistrareRepository.findAll(Sort.by("id"));
        return codInregistrares.stream()
                .map(codInregistrare -> mapToDTO(codInregistrare, new CodInregistrareDTO()))
                .toList();
    }

    public CodInregistrareDTO get(final UUID id) {
        return codInregistrareRepository.findById(id)
                .map(codInregistrare -> mapToDTO(codInregistrare, new CodInregistrareDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public List<CodInregistrareDTO> getCoduriByMedic(UUID medicId) {
        List<CodInregistrare> coduri = codInregistrareRepository.findByGeneratDeId(medicId);
        return coduri.stream()
                .map(cod -> mapToDTO(cod, new CodInregistrareDTO()))
                .collect(Collectors.toList());
    }

    public UUID create(final CodInregistrareDTO codInregistrareDTO) {
        final CodInregistrare codInregistrare = new CodInregistrare();
        mapToEntity(codInregistrareDTO, codInregistrare);
        return codInregistrareRepository.save(codInregistrare).getId();
    }

    public void update(final UUID id, final CodInregistrareDTO codInregistrareDTO) {
        final CodInregistrare codInregistrare = codInregistrareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(codInregistrareDTO, codInregistrare);
        codInregistrareRepository.save(codInregistrare);
    }

    public void delete(final UUID id) {
        final CodInregistrare codInregistrare = codInregistrareRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        codInregistrareRepository.delete(codInregistrare);
    }

    public GenerareCodResponse generareCodInregistrare(GenerareCodRequest request) {
        try {
            System.out.println("=== SERVICE: Generare cod pentru " + request.getEmailDestinatar());

            // Validare
            if (request.getRolDestinatar() == null) {
                return new GenerareCodResponse("Rolul destinatarului este obligatoriu");
            }

            // Verificam medicul care generează
            Utilizatori medic = utilizatoriRepository.findById(request.getMedicId())
                    .orElseThrow(() -> new NotFoundException("Medicul nu a fost găsit"));

            if (medic.getRol() != RoleType.MEDIC && medic.getRol() != RoleType.ADMIN) {
                return new GenerareCodResponse("Doar medicii sau administratorii pot genera coduri");
            }


            Optional<Utilizatori> utilizatorExistent = utilizatoriRepository.findByEmail(request.getEmailDestinatar());
            Utilizatori utilizator;

            if (utilizatorExistent.isPresent()) {
                // Utilizatorul existent
                utilizator = utilizatorExistent.get();
                System.out.println("Utilizator există deja în baza locală: " + utilizator.getId());

                // Verificare rol
                if (utilizator.getRol() != request.getRolDestinatar()) {
                    return new GenerareCodResponse("Există deja un utilizator cu acest email, dar cu alt rol: " + utilizator.getRol());
                }

                // Verifică dacă există deja cod neutilizat pentru el
                List<CodInregistrare> coduriExistent = codInregistrareRepository
                        .findNeutilizatByEmail(request.getEmailDestinatar());
                if (!coduriExistent.isEmpty()) {
                    return new GenerareCodResponse("Există deja un cod neutilizat pentru acest utilizator");
                }

            } else {

                System.out.println("Utilizatorul nu există, se creează nou...");

                // Creare utilizaotr nou
                utilizator = new Utilizatori();
                utilizator.setEmail(request.getEmailDestinatar());
                utilizator.setRol(request.getRolDestinatar());

                if (request instanceof GenerareCodRequest) {

                    GenerareCodRequest extendedRequest = (GenerareCodRequest) request;
                    utilizator.setNume(extendedRequest.getNumeDestinatar());
                    utilizator.setPrenume(extendedRequest.getPrenumeDestinatar());
                } else {

                    String emailPrefix = request.getEmailDestinatar().split("@")[0];
                    utilizator.setNume(emailPrefix);
                    utilizator.setPrenume("Nou");
                }

                // Salvare utilizator
                utilizator = utilizatoriRepository.save(utilizator);
                System.out.println("Utilizator nou creat cu ID: " + utilizator.getId());

                // Dacă e PACIENT, creează și înregistrarea în tabela pacienți
                // În service, înlocuiește secțiunea de creare pacient cu:

                if (request.getRolDestinatar() == RoleType.PACIENT) {
                    System.out.println("=== DEBUG: Procesare PACIENT ===");
                    System.out.println("CNP din request: '" + request.getCnpDestinatar() + "'");
                    System.out.println("CNP is null? " + (request.getCnpDestinatar() == null));
                    System.out.println("CNP is empty? " + (request.getCnpDestinatar() != null && request.getCnpDestinatar().trim().isEmpty()));

                    if (request.getCnpDestinatar() != null && !request.getCnpDestinatar().trim().isEmpty()) {
                        System.out.println("CNP valid, încerc creare pacient...");

                        try {
                            // 1. Creează pacient
                            Pacienti pacient = new Pacienti();
                            System.out.println("✅ Pas 1: Obiect Pacienti creat");

                            // 2. Setează CNP
                            String cnp = request.getCnpDestinatar().trim();
                            pacient.setCnp(cnp);
                            System.out.println("✅ Pas 2: CNP setat: " + cnp);

                            // 3. Setează utilizator
                            pacient.setUser(utilizator);
                            System.out.println("✅ Pas 3: User asociat: " + utilizator.getId() + " - " + utilizator.getEmail());

                            // 4. Setează medic = null (dacă ai nullable = true)
                            if (medic.getMedic() != null) {
                                // Utilizatorul medic are înregistrare în tabela Medici
                                pacient.setMedic(medic.getMedic());
                                //System.out.println("✅ Medic asociat (din relație): " + medic.getMedic().getId());
                            } else {
                                System.out.println("⚠️ Utilizatorul medic nu are înregistrare în tabela Medici");

                                // Opțiune 1: Creează înregistrare Medici pentru acest utilizator
                                try {
                                    Medici medicNou = new Medici();
                                    medicNou.setUser(medic);

                                    // Salvează medicul (nevoie de MediciRepository injectat)
                                    // Medici savedMedic = mediciRepository.save(medicNou);
                                    // pacient.setMedic(savedMedic);
                                    // System.out.println("✅ Medic creat pentru utilizator");

                                    System.out.println("⚠️ MediciRepository nu este disponibil, setează medic = null");
                                    pacient.setMedic(null);

                                } catch (Exception e) {
                                    System.out.println("❌ Eroare creare medic: " + e.getMessage());
                                    pacient.setMedic(null);
                                }
                            }

                            // 5. Încearcă să salvezi
                            System.out.println("Încearcă save...");
                            Pacienti savedPacient = pacientiRepository.save(pacient);
                            System.out.println("🎉 PACIENT SALVAT CU SUCCES! ID: " + savedPacient.getId());
                            System.out.println("CNP salvat: " + savedPacient.getCnp());
                            System.out.println("User ID salvat: " + (savedPacient.getUser() != null ? savedPacient.getUser().getId() : "null"));

                        } catch (Exception e) {
                            System.out.println("❌ EXCEPTIE la creare pacient:");
                            System.out.println("Mesaj: " + e.getMessage());
                            System.out.println("Cauza: " + e.getCause());
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("⚠️ CNP invalid - nu se creează pacient");
                    }
                } else {
                    System.out.println("⚠️ Rolul nu este PACIENT, skipping pacient creation");
                }
            }

            // Generează cod
            String codUnic = generareCodUnic();
            System.out.println("✅ Cod unic generat: " + codUnic);

            //Creeare codul și îl atribuie utilizatorului
            CodInregistrare cod = new CodInregistrare();
            cod.setCodUnic(codUnic);
            cod.setStatus(CodInregistrare.StatusCod.NEUTILIZAT);
            cod.setGeneratDe(medic);
            cod.setEmailDestinatar(request.getEmailDestinatar());
            cod.setCnpDestinatar(request.getCnpDestinatar());
            cod.setRolDestinatar(request.getRolDestinatar());
            cod.setAtribuit(utilizator); // ← ATRIBUIT CU UTILIZATORUL (existent sau nou)

            codInregistrareRepository.save(cod);
            System.out.println("✅ Cod salvat în baza de date cu ID: " + cod.getId());

            //Trimite email
            //trimiteEmailCod(codUnic, request.getEmailDestinatar(), medic.getNumeComplet());

            return new GenerareCodResponse(codUnic, request.getEmailDestinatar());

        } catch (Exception e) {
            System.out.println("❌ Eroare la generare cod: " + e.getMessage());
            e.printStackTrace();
            return new GenerareCodResponse("Eroare internă la generare cod: " + e.getMessage());
        }
    }
    private String generareCodUnic() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        String codGenerat = sb.toString();
        System.out.println("Generat cod: " + codGenerat);

        // Verifică dacă codul nu există deja
        if (codInregistrareRepository.existsByCodUnic(codGenerat)) {
            System.out.println("Cod există deja, generăm altul...");
            return generareCodUnic(); // recursiv până găsește unic
        }

        return codGenerat;
    }

    private void trimiteEmailCod(String cod, String emailDestinatar, String numeMedic) {
        try {
            System.out.println("=== TRIMITERE EMAIL ===");
            System.out.println("Cod: " + cod);
            System.out.println("Destinatar: " + emailDestinatar);
            System.out.println("Medic: " + numeMedic);

            // de implementat logica de trmitere email


        } catch (Exception e) {
            System.out.println("Eroare la trimitere email: " + e.getMessage());
        }
    }



    private CodInregistrareDTO mapToDTO(final CodInregistrare codInregistrare,
                                        final CodInregistrareDTO codInregistrareDTO) {
        codInregistrareDTO.setId(codInregistrare.getId());
        codInregistrareDTO.setCodUnic(codInregistrare.getCodUnic());
        codInregistrareDTO.setStatus(codInregistrare.getStatus());
        codInregistrareDTO.setCreatedAt(codInregistrare.getCreatedAt());
        codInregistrareDTO.setGeneratDe(codInregistrare.getGeneratDe() == null ? null : codInregistrare.getGeneratDe().getId());
        codInregistrareDTO.setAtribuit(codInregistrare.getAtribuit() == null ? null : codInregistrare.getAtribuit().getId());

        codInregistrareDTO.setEmailDestinatar(codInregistrare.getEmailDestinatar());
        codInregistrareDTO.setCnpDestinatar(codInregistrare.getCnpDestinatar());
        codInregistrareDTO.setRolDestinatar(codInregistrare.getRolDestinatar());

        return codInregistrareDTO;
    }

    private CodInregistrare mapToEntity(final CodInregistrareDTO codInregistrareDTO,
                                        final CodInregistrare codInregistrare) {
        codInregistrare.setCodUnic(codInregistrareDTO.getCodUnic());
        codInregistrare.setStatus(codInregistrareDTO.getStatus());
        codInregistrare.setCreatedAt(codInregistrareDTO.getCreatedAt());

        final Utilizatori generatDe = codInregistrareDTO.getGeneratDe() == null ? null : utilizatoriRepository.findById(codInregistrareDTO.getGeneratDe())
                .orElseThrow(() -> new NotFoundException("generatDe not found"));
        codInregistrare.setGeneratDe(generatDe);

        final Utilizatori atribuit = codInregistrareDTO.getAtribuit() == null ? null : utilizatoriRepository.findById(codInregistrareDTO.getAtribuit())
                .orElseThrow(() -> new NotFoundException("atribuit not found"));
        codInregistrare.setAtribuit(atribuit);

        codInregistrare.setEmailDestinatar(codInregistrareDTO.getEmailDestinatar());
        codInregistrare.setCnpDestinatar(codInregistrareDTO.getCnpDestinatar());
        codInregistrare.setRolDestinatar(codInregistrareDTO.getRolDestinatar());

        return codInregistrare;
    }

    @EventListener(BeforeDeleteUtilizatori.class)
    public void on(final BeforeDeleteUtilizatori event) {
        final ReferencedException referencedException = new ReferencedException();
        final CodInregistrare generatDeCodInregistrare = codInregistrareRepository.findFirstByGeneratDeId(event.getId());
        if (generatDeCodInregistrare != null) {
            referencedException.setKey("utilizatori.codInregistrare.generatDe.referenced");
            referencedException.addParam(generatDeCodInregistrare.getId());
            throw referencedException;
        }
        final CodInregistrare atribuitCodInregistrare = codInregistrareRepository.findFirstByAtribuitId(event.getId());
        if (atribuitCodInregistrare != null) {
            referencedException.setKey("utilizatori.codInregistrare.atribuit.referenced");
            referencedException.addParam(atribuitCodInregistrare.getId());
            throw referencedException;
        }
    }

}
