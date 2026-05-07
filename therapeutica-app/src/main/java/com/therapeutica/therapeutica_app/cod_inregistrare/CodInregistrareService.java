package com.therapeutica.therapeutica_app.cod_inregistrare;

import com.therapeutica.therapeutica_app.cod_inregistrare.dto.CodInregistrareDTO;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodRequest;
import com.therapeutica.therapeutica_app.cod_inregistrare.dto.GenerareCodResponse;
import com.therapeutica.therapeutica_app.events.BeforeDeleteUtilizatori;
import com.therapeutica.therapeutica_app.notificari.external.WhatsAppService;
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
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CodInregistrareService {

    private static final Logger logger = LoggerFactory.getLogger(CodInregistrareService.class);

    private final CodInregistrareRepository codInregistrareRepository;
    private final UtilizatoriRepository utilizatoriRepository;
    private final WhatsAppService whatsappService;

    @Transactional
    public GenerareCodResponse generareCodInregistrare(GenerareCodRequest request) {
        logger.info("=== START SERVICE: Generare cod pentru {} ===", request.getEmailDestinatar());

        Utilizatori medicUser = utilizatoriRepository.findById(request.getMedicId())
                .orElseThrow(() -> new NotFoundException("Medicul nu a fost găsit."));

        if (medicUser.getRol() != RoleType.MEDIC && medicUser.getRol() != RoleType.ADMIN) {
            return new GenerareCodResponse("Eroare: Lipsă permisiuni medic.");
        }

        Optional<Utilizatori> utilizatorExistent = utilizatoriRepository.findByEmail(request.getEmailDestinatar());
        if (utilizatorExistent.isPresent()) {
            return new GenerareCodResponse("Eroare: Există deja un cont înregistrat cu acest email.");
        }

        if (!codInregistrareRepository.findNeutilizatByEmail(request.getEmailDestinatar()).isEmpty()) {
            return new GenerareCodResponse("Eroare: Există deja un cod activ pentru acest email.");
        }

        String codUnicStr = generareCodUnic();
        CodInregistrare codEntity = new CodInregistrare();
        codEntity.setCodUnic(codUnicStr);
        codEntity.setStatus(CodInregistrare.StatusCod.NEUTILIZAT);
        codEntity.setGeneratDe(medicUser);
        codEntity.setAtribuit(null);
        codEntity.setEmailDestinatar(request.getEmailDestinatar());
        codEntity.setCnpDestinatar(request.getCnpDestinatar());
        codEntity.setTelefonDestinatar(request.getTelefonDestinatar());
        codEntity.setRolDestinatar(request.getRolDestinatar());

        codInregistrareRepository.save(codEntity);

        GenerareCodResponse response = GenerareCodResponse.builder()
                .codUnic(codUnicStr)
                .emailDestinatar(request.getEmailDestinatar())
                .status(CodInregistrare.StatusCod.NEUTILIZAT)
                .mesaj("Cod generat cu succes")
                .build();

        if (request.getTelefonDestinatar() != null && !request.getTelefonDestinatar().trim().isEmpty()) {
            try {
                String link = whatsappService.genereazaLinkMesaj(request.getTelefonDestinatar(), codUnicStr);
                response.setWhatsappLink(link);
            } catch (IllegalArgumentException e) {
                logger.warn("Eșec la generarea link-ului WhatsApp pentru {}: {}", request.getTelefonDestinatar(), e.getMessage());
            }
        }

        return response;
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
        dto.setTelefonDestinatar(cod.getTelefonDestinatar());
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
        cod.setTelefonDestinatar(dto.getTelefonDestinatar());
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