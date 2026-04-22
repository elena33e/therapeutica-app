package com.therapeutica.therapeutica_app.atribuire_chestionar;

import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.chestionare.ChestionareRepository;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.medici.MediciRepository;
import com.therapeutica.therapeutica_app.notificari.NotificareListenerService;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class AtribuireChestionarService {

    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final PacientiRepository pacientiRepository;
    private final MediciRepository mediciRepository;
    private final ChestionareRepository chestionareRepository;
    private final NotificareListenerService notificareListenerService;

    public AtribuireChestionarService(
            final RaspunsuriChestionareRepository raspunsuriChestionareRepository,
            final PacientiRepository pacientiRepository,
            final MediciRepository mediciRepository,
            final ChestionareRepository chestionareRepository,
            final NotificareListenerService notificareListenerService) {
        this.raspunsuriChestionareRepository = raspunsuriChestionareRepository;
        this.pacientiRepository = pacientiRepository;
        this.mediciRepository = mediciRepository;
        this.chestionareRepository = chestionareRepository;
        this.notificareListenerService = notificareListenerService;
    }

    /**
     * Atribuie unul sau mai multe chestionare unui pacient
     */
    public List<UUID> atribuiChestionarePacientului(AtribuireChestionarRequestDTO requestDTO) {
        try {
            // Încearcă să găsești pacientul cu acest ID
            Optional<Pacienti> byId = pacientiRepository.findById(requestDTO.getPacientId());
            System.out.println("🔍 Found by pacient.id? " + byId.isPresent());

            if (!byId.isPresent()) {
                // Încearcă să găsești după user_id
                Optional<Pacienti> byUserId = pacientiRepository.findByUserId(requestDTO.getPacientId());
                System.out.println("🔍 Found by user.id? " + byUserId.isPresent());

                if (byUserId.isPresent()) {
                    System.out.println("🚨🚨🚨 PROBLEM IDENTIFIED! 🚨🚨🚨");
                    System.out.println("   - DTO has user.id: " + requestDTO.getPacientId());
                    System.out.println("   - But should have pacient.id: " + byUserId.get().getId());
                    System.out.println("   - Patient user_id: " +
                            (byUserId.get().getUser() != null ? byUserId.get().getUser().getId() : "null"));


                    requestDTO.setPacientId(byUserId.get().getId());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error in debug: " + e.getMessage());
        }

        System.out.println("   - pacientId: " + requestDTO.getPacientId());
        System.out.println("   - medicId: " + requestDTO.getMedicId());
        System.out.println("   - chestionareIds: " +
                (requestDTO.getChestionareIds() != null ? requestDTO.getChestionareIds() : "NULL"));

        if (requestDTO.getChestionareIds() != null) {
            System.out.println("   - Number of IDs: " + requestDTO.getChestionareIds().size());
        }

        // Validare precondiții
        if (requestDTO.getChestionareIds() == null || requestDTO.getChestionareIds().isEmpty()) {
            System.err.println("❌ ERROR: No chestionareIds in DTO!");
            throw new IllegalArgumentException("Trebuie selectat cel puțin un chestionar");
        }

        try {
            // Verifică medicul
            System.out.println("🔍 Looking for medic with ID: " + requestDTO.getMedicId());
            final Medici medic = mediciRepository.findById(requestDTO.getMedicId())
                    .orElseThrow(() -> {
                        System.err.println("❌ Medic NOT FOUND: " + requestDTO.getMedicId());
                        return new NotFoundException("Medic not found");
                    });
            System.out.println("✅ Medic found: " + medic.getUserId());

            // Verifică pacientul
            System.out.println("🔍 Looking for pacient with ID: " + requestDTO.getPacientId());
            final Pacienti pacient = pacientiRepository.findById(requestDTO.getPacientId())
                    .orElseThrow(() -> {
                        System.err.println("❌ Pacient NOT FOUND by ID: " + requestDTO.getPacientId());
                        // Încearcă și alternative pentru debugging
                        System.err.println("🔍 Trying to find by user_id instead...");
                        Optional<Pacienti> byUser = pacientiRepository.findByUserId(requestDTO.getPacientId());
                        if (byUser.isPresent()) {
                            System.err.println("⚠️ Found by user_id! But DTO should send pacient.id not user.id");
                        }
                        return new NotFoundException("Pacient not found with ID: " + requestDTO.getPacientId());
                    });

            System.out.println("✅ Pacient found:");
            System.out.println("   - Pacient ID (from pacienti table): " + pacient.getId());
            System.out.println("   - User ID (FK to utilizatori): " +
                    (pacient.getUser() != null ? pacient.getUser().getId() : "null"));

            // Verifică dacă medicul are drepturi asupra pacientului
            System.out.println("🔍 Checking if pacient belongs to medic...");
            System.out.println("   - Pacient medic ID: " +
                    (pacient.getMedic() != null ? pacient.getMedic().getUserId() : "NULL"));
            System.out.println("   - Request medic ID: " + medic.getUserId());

            boolean pacientApartineMedicului = estePacientAlMedicului(pacient, medic);
            System.out.println("✅ Patient belongs to medic: " + pacientApartineMedicului);

            if (!pacientApartineMedicului) {
                throw new SecurityException("Pacientul nu este asociat medicului");
            }

            List<UUID> raspunsuriCreateIds = new ArrayList<>();

            // Pentru fiecare chestionar selectat
            System.out.println("🔍 Processing " + requestDTO.getChestionareIds().size() + " chestionare...");
            for (UUID chestionarId : requestDTO.getChestionareIds()) {
                System.out.println("   🔄 Processing chestionar ID: " + chestionarId);

                final Chestionare chestionar = chestionareRepository.findById(chestionarId)
                        .orElseThrow(() -> {
                            System.err.println("❌ Chestionar NOT FOUND: " + chestionarId);
                            return new NotFoundException("Chestionar not found");
                        });
                System.out.println("     ✅ Chestionar found: " + chestionar.getNume());

                // Verifică dacă chestionarul nu a fost deja atribuit (și necompletat)
                boolean existaDeja = existaChestionarNecompletat(pacient.getId(), chestionarId);
                System.out.println("     🔍 Chestionar already assigned (not completed): " + existaDeja);

                if (!existaDeja) {
                    System.out.println("     🆕 Creating new association...");

                    // Creează un răspuns chestionar (asociere)
                    RaspunsuriChestionare raspuns = new RaspunsuriChestionare();
                    raspuns.setPacient(pacient);
                    raspuns.setMedic(medic);
                    raspuns.setChestionar(chestionar);
                    raspuns.setStatus(RaspunsuriChestionare.StatusRaspuns.NECOMPLETAT);
                    raspuns.setCompletatLa(null);
                    raspuns.setScorTotalGeneral(null);

                    System.out.println("     💾 Saving to database...");
                    RaspunsuriChestionare saved = raspunsuriChestionareRepository.save(raspuns);
                    System.out.println("     ✅ Saved with ID: " + saved.getId());
                    raspunsuriCreateIds.add(saved.getId());

                    // Verifică imediat dacă s-a salvat
                    boolean existsInDb = raspunsuriChestionareRepository.existsById(saved.getId());
                    System.out.println("     🔍 Verification - exists in DB: " + existsInDb);
                } else {
                    System.out.println("     ⚠️ Skipping - already assigned");
                }
            }

            System.out.println("✅ Total created associations: " + raspunsuriCreateIds.size());

            if (!raspunsuriCreateIds.isEmpty()) {
                System.out.println("✅ Created response IDs: " + raspunsuriCreateIds);
            } else {
                System.out.println("⚠️ No new associations created (all were already assigned)");
            }

            // Trimite notificare către pacient
//            System.out.println("🔍 Sending notification to patient...");
//            try {
//                trimiteNotificarePacient(pacient, medic, requestDTO);
//                System.out.println("✅ Notification sent");
//            } catch (Exception e) {
//                System.err.println("⚠️ Failed to send notification: " + e.getMessage());
//            }

            return raspunsuriCreateIds;

        } catch (Exception e) {
            System.err.println("❌❌❌ ERROR in atribuiChestionarePacientului:");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Exception type: " + e.getClass().getName());
            e.printStackTrace();
            throw e;
        }
    }
    /**
     * Verifică dacă pacientul este asociat medicului
     */
    private boolean estePacientAlMedicului(Pacienti pacient, Medici medic) {
        // Verifică dacă pacientul are medicul setat sau dacă medicul apare în lista de pacienți
        return pacient.getMedic() != null && pacient.getMedic().getUserId().equals(medic.getUserId());
    }

    /**
     * Verifică dacă există deja un chestionar necompletat pentru același pacient
     */
    private boolean existaChestionarNecompletat(UUID pacientId, UUID chestionarId) {
        return raspunsuriChestionareRepository
                .existsByPacientIdAndChestionarIdAndStatusNecompletat(pacientId, chestionarId);
    }

    /**
     * Trimite notificare către pacient
     */
    private void trimiteNotificarePacient(Pacienti pacient, Medici medic,
                                          AtribuireChestionarRequestDTO requestDTO) {
        if (pacient.getUser() != null) {
            Utilizatori pacientUser = pacient.getUser();
            Utilizatori medicUser = medic.getUser();

            String subiect = "Chestionar nou atribuit";
            String mesaj = String.format(
                    "Mesaj test trimis",
                    pacientUser.getPrenume(),
                    pacientUser.getNume(),
                    medicUser.getPrenume(),
                    medicUser.getNume()
//                    requestDTO.getMesajPersonalizat() != null ?
//                            "Mesaj personalizat de la medic:\n" + requestDTO.getMesajPersonalizat() : ""
            );

            //notificareListenerService.trimiteEmail(pacientUser.getEmail(), subiect, mesaj);
        }
    }

    /**
     * Obține chestionarele disponibile pentru atribuire (care nu sunt deja atribuite și necompletate)
     */
    public List<Chestionare> getChestionareDisponibilePentruPacient(UUID pacientId) {
        System.out.println("Getting available questionnaires for patient: " + pacientId);

        // Folosește metoda corectă din repository
        // Obține chestionarele deja atribuite (indiferent de status)
        List<UUID> chestionareAtribuite = raspunsuriChestionareRepository
                .findByPacientIdOrderByCompletatLa(pacientId)
                .stream()
                .map(rc -> rc.getChestionar().getId())
                .distinct() // Elimină duplicatele
                .toList();

        System.out.println("   - Already assigned: " + chestionareAtribuite.size() + " questionnaires");

        // Obține toate chestionarele
        List<Chestionare> toateChestionarele = chestionareRepository.findAll();
        System.out.println("   - Total questionnaires in system: " + toateChestionarele.size());

        // Filtrează cele care nu sunt deja atribuite
        List<Chestionare> disponibile = toateChestionarele.stream()
                .filter(c -> !chestionareAtribuite.contains(c.getId()))
                .toList();

        System.out.println("   - Available for assignment: " + disponibile.size() + " questionnaires");

        return disponibile;
    }

    /**
     * Verifică dacă pacientul este asociat medicului (public method pentru API)
     */
    public boolean estePacientAsociatMedicului(UUID pacientId, UUID medicId) {
        final Pacienti pacient = pacientiRepository.findById(pacientId)
                .orElseThrow(() -> new NotFoundException("Pacient not found"));

        final Medici medic = mediciRepository.findById(medicId)
                .orElseThrow(() -> new NotFoundException("Medic not found"));

        return estePacientAlMedicului(pacient, medic);
    }

    /**
     * Obține istoricul chestionarelor pentru un pacient
     */

    public List<RaspunsuriChestionare> getIstoricChestionarePacient(UUID pacientId) {

        return raspunsuriChestionareRepository.findByPacientIdWithRelations(pacientId);
    }


}