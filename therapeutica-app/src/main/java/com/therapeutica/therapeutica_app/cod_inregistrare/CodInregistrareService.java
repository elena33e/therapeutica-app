package com.therapeutica.therapeutica_app.cod_inregistrare;

import com.therapeutica.therapeutica_app.cod_inregistrare.dto.CodInregistrareDTO;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodRequest;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodResponse;
import com.therapeutica.therapeutica_app.events.BeforeDeleteUtilizatori;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.util.ReferencedException;
import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import com.therapeutica.therapeutica_app.utilizatori.UtilizatoriRepository;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CodInregistrareService {

    private final CodInregistrareRepository codInregistrareRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final PacientiRepository pacientiRepository;

    // --- LOGICA SPECIALĂ (PENTRU CONTROLLER-UL TĂU) ---

    @Transactional
    public GenerareCodResponse generareCodInregistrare(GenerareCodRequest request) {
        System.out.println("=== START SERVICE: Generare cod pentru " + request.getEmailDestinatar() + " ===");

        Utilizatori medicUser = utilizatoriRepository.findById(request.getMedicId())
                .orElseThrow(() -> new NotFoundException("Medicul nu a fost găsit."));

        if (medicUser.getRol() != RoleType.MEDIC && medicUser.getRol() != RoleType.ADMIN) {
            return new GenerareCodResponse("Eroare: Lipsă permisiuni medic.");
        }

        Utilizatori destinatar = utilizatoriRepository.findByEmail(request.getEmailDestinatar())
                .orElseGet(() -> {
                    Utilizatori nou = new Utilizatori();
                    nou.setEmail(request.getEmailDestinatar());
                    nou.setRol(request.getRolDestinatar());
                    nou.setNume(request.getNumeDestinatar());
                    nou.setPrenume(request.getPrenumeDestinatar());
                    return utilizatoriRepository.save(nou);
                });

        if (destinatar.getRol() != request.getRolDestinatar()) {
            return new GenerareCodResponse("Eroare: Conflict rol utilizator existent.");
        }

        if (!codInregistrareRepository.findNeutilizatByEmail(request.getEmailDestinatar()).isEmpty()) {
            return new GenerareCodResponse("Există deja un cod activ pentru acest email.");
        }

        if (request.getRolDestinatar() == RoleType.PACIENT) {
            handlePacientCreation(destinatar, medicUser, request.getCnpDestinatar());
        }

        String codUnicStr = generareCodUnic();
        CodInregistrare codEntity = new CodInregistrare();
        codEntity.setCodUnic(codUnicStr);
        codEntity.setStatus(CodInregistrare.StatusCod.NEUTILIZAT);
        codEntity.setGeneratDe(medicUser);
        codEntity.setAtribuit(destinatar);
        codEntity.setEmailDestinatar(request.getEmailDestinatar());
        codEntity.setCnpDestinatar(request.getCnpDestinatar());
        codEntity.setRolDestinatar(request.getRolDestinatar());

        codInregistrareRepository.save(codEntity);

        return new GenerareCodResponse(
                codUnicStr,
                request.getEmailDestinatar(),
                CodInregistrare.StatusCod.NEUTILIZAT,
                "Cod generat cu succes"
        );
    }

    // --- LOGICA CRUD (PENTRU RESOURCE / SWAGGER) ---

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
        codInregistrareRepository.deleteById(id);
    }

    public List<CodInregistrareDTO> findAll() {
        return codInregistrareRepository.findAll(Sort.by("id")).stream()
                .map(cod -> mapToDTO(cod, new CodInregistrareDTO()))
                .toList();
    }

    public CodInregistrareDTO get(final UUID id) {
        return codInregistrareRepository.findById(id)
                .map(cod -> mapToDTO(cod, new CodInregistrareDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public List<CodInregistrareDTO> getCoduriByMedic(UUID medicId) {
        return codInregistrareRepository.findByGeneratDeId(medicId).stream()
                .map(cod -> mapToDTO(cod, new CodInregistrareDTO()))
                .collect(Collectors.toList());
    }

    // --- METODE PRIVATE / HELPER ---

    private void handlePacientCreation(Utilizatori destinatar, Utilizatori medicUser, String cnp) {
        if (cnp == null || cnp.trim().isEmpty()) return;
        if (pacientiRepository.existsByUserId(destinatar.getId())) return;

        Pacienti pacient = new Pacienti();
        pacient.setUser(destinatar);
        pacient.setCnp(cnp.trim());
        if (medicUser.getMedic() != null) {
            pacient.setMedic(medicUser.getMedic());
        }
        pacientiRepository.save(pacient);
    }

    private String generareCodUnic() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String cod;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
            cod = sb.toString();
        } while (codInregistrareRepository.existsByCodUnic(cod));
        return cod;
    }

    private CodInregistrareDTO mapToDTO(final CodInregistrare cod, final CodInregistrareDTO dto) {
        dto.setId(cod.getId());
        dto.setCodUnic(cod.getCodUnic());
        dto.setStatus(cod.getStatus());
        dto.setGeneratDe(cod.getGeneratDe() != null ? cod.getGeneratDe().getId() : null);
        dto.setAtribuit(cod.getAtribuit() != null ? cod.getAtribuit().getId() : null);
        dto.setEmailDestinatar(cod.getEmailDestinatar());
        dto.setCnpDestinatar(cod.getCnpDestinatar());
        dto.setRolDestinatar(cod.getRolDestinatar());
        return dto;
    }

    private void mapToEntity(final CodInregistrareDTO dto, final CodInregistrare cod) {
        cod.setCodUnic(dto.getCodUnic());
        cod.setStatus(dto.getStatus());
        if (dto.getGeneratDe() != null) {
            cod.setGeneratDe(utilizatoriRepository.findById(dto.getGeneratDe()).orElse(null));
        }
        if (dto.getAtribuit() != null) {
            cod.setAtribuit(utilizatoriRepository.findById(dto.getAtribuit()).orElse(null));
        }
        cod.setEmailDestinatar(dto.getEmailDestinatar());
        cod.setCnpDestinatar(dto.getCnpDestinatar());
        cod.setRolDestinatar(dto.getRolDestinatar());
    }

    @EventListener(BeforeDeleteUtilizatori.class)
    public void on(final BeforeDeleteUtilizatori event) {
        if (codInregistrareRepository.findFirstByGeneratDeId(event.getId()) != null ||
                codInregistrareRepository.findFirstByAtribuitId(event.getId()) != null) {
            throw new ReferencedException();
        }
    }
}