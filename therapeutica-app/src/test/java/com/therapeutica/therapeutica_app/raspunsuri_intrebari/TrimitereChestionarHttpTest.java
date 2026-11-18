package com.therapeutica.therapeutica_app.raspunsuri_intrebari;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import com.therapeutica.therapeutica_app.raport_chestionar.RezultatScoring;
import com.therapeutica.therapeutica_app.raport_chestionar.RezultatScoringRepository;

@SpringBootTest(properties = {
        "spring.config.location=classpath:/application-http-test.yml"
})
@AutoConfigureMockMvc
@ActiveProfiles("http-test")
@Testcontainers
class TrimitereChestionarHttpTest {

    private static final String PAROLA_TEST = "Parola-fictiva-HTTP-123!";

    private static final Path DIRECTOR_DOCUMENTE = Path.of(
            System.getProperty("java.io.tmpdir"),
            "therapeutica-http-test-" + UUID.randomUUID()
    ).toAbsolutePath();


    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.1")
                    .withDatabaseName("therapeutica_http_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureazaStocarea(DynamicPropertyRegistry registry) {
        registry.add(
                "app.storage.documents-dir",
                DIRECTOR_DOCUMENTE::toString
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Necesar pentru contextul complet; emailurile nu sunt trimise.
    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private RaspunsuriIntrebariRepository raspunsuriRepository;

    @MockitoSpyBean
    private RezultatScoringRepository rezultateScoringRepository;

    private final UUID medicUserId = UUID.randomUUID();
    private final UUID pacientUserId = UUID.randomUUID();
    private final UUID pacientId = UUID.randomUUID();
    private final UUID chestionarId = UUID.randomUUID();
    private final UUID categorieId = UUID.randomUUID();
    private final UUID intrebareId = UUID.randomUUID();
    private final UUID atribuireId = UUID.randomUUID();

    private final String emailPacient =
            "pacient-" + pacientUserId + "@example.test";

    @BeforeEach
    void pregatesteDatele() {
        // Verificare înainte de introducerea oricăror date de test.
        assertThat(jdbc.queryForObject(
                "SELECT current_database()",
                String.class
        )).isEqualTo(postgres.getDatabaseName());

        String parolaCodificata = passwordEncoder.encode(PAROLA_TEST);

        creeazaUtilizator(
                medicUserId,
                "medic-" + medicUserId + "@example.test",
                "MEDIC",
                parolaCodificata
        );

        creeazaUtilizator(
                pacientUserId,
                emailPacient,
                "PACIENT",
                parolaCodificata
        );

        jdbc.update("""
                INSERT INTO public.medici (user_id, specializare)
                VALUES (?, 'Specializare de test')
                """,
                medicUserId
        );

        jdbc.update("""
                INSERT INTO public.pacienti
                    (id, user_id, medic_id, sex)
                VALUES (?, ?, ?, 'NEPRECIZAT')
                """,
                pacientId,
                pacientUserId,
                medicUserId
        );

        jdbc.update("""
                INSERT INTO public.chestionare (id, nume)
                VALUES (?, 'Chestionar HTTP de test')
                """,
                chestionarId
        );

        jdbc.update("""
                INSERT INTO public.categorii_chestionare
                    (id, chestionar_id, nume, ordine,
                     este_evaluabila, sex_tinta)
                VALUES (?, ?, 'Categorie de test', 1, true, 'AMBELE')
                """,
                categorieId,
                chestionarId
        );

        jdbc.update("""
                INSERT INTO public.intrebari
                    (id, categorie_id, text_intrebare,
                     tip_intrebare, obligatorie, ordine)
                VALUES (?, ?, 'Simptom de test', 'SCOR_0_3', true, 1)
                """,
                intrebareId,
                categorieId
        );

        jdbc.update("""
                INSERT INTO public.raspunsuri_chestionare
                    (id, pacient_id, medic_id, chestionar_id,
                     status, completat_la, scor_total_general)
                VALUES (?, ?, ?, ?, 'NECOMPLETAT', NULL, NULL)
                """,
                atribuireId,
                pacientId,
                medicUserId,
                chestionarId
        );
    }

    @Test
    void formularulFaraRaspunsObligatoriuEsteRespinsFaraSalvare()
            throws Exception {

        verificaChestionarNecompletat();

        // Autentificare prin configurația reală de securitate.
        MvcResult autentificare = mockMvc.perform(
                        post("/process-login")
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param("username", emailPacient)
                                .param("password", PAROLA_TEST)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated()
                        .withUsername(emailPacient)
                        .withRoles("PACIENT"))
                .andExpect(redirectedUrl(
                        "/pacient/dashboard/" + pacientUserId
                ))
                .andReturn();

        MockHttpSession sesiune = (MockHttpSession)
                autentificare.getRequest().getSession(false);

        assertThat(sesiune).isNotNull();
        assertThat(sesiune.getAttribute("userId"))
                .isEqualTo(pacientUserId.toString());
        assertThat(sesiune.getAttribute("userRole"))
                .isEqualTo("PACIENT");

        String cheieIntrebare = "intrebare_" + intrebareId;

        // Nu trimitem parametrul întrebării obligatorii.
        mockMvc.perform(
                        post(
                                "/raspunsuri/completare/{id}/salveaza",
                                atribuireId
                        )
                                .session(sesiune)
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                )
                .andExpect(handler().methodName("salveazaRaspunsuri"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/chestionare/completare/" + atribuireId
                ))
                .andExpect(flash().attribute(
                        "eroriValidare",
                        hasEntry(
                                cheieIntrebare,
                                "Simptom de test: răspuns obligatoriu."
                        )
                ))
                .andExpect(flash().attribute(
                        "raspunsuriFormular",
                        Map.of()
                ))
                .andExpect(flash().attributeCount(2));

        // Citire directă din DB după încheierea cererii HTTP.
        verificaChestionarNecompletat();

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM public.rezultate_scoring
                WHERE raspuns_chestionar_id = ?
                """,
                Long.class,
                atribuireId
        )).isZero();
    }

    private void creeazaUtilizator(
            UUID id,
            String email,
            String rol,
            String parolaCodificata) {

        jdbc.update("""
                INSERT INTO public.utilizatori
                    (id, created_at, email, nume, prenume, rol, parola)
                VALUES (?, CURRENT_TIMESTAMP, ?, 'Test', 'HTTP', ?, ?)
                """,
                id,
                email,
                rol,
                parolaCodificata
        );
    }

    private void verificaChestionarNecompletat() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM public.raspunsuri_intrebari
                WHERE raspuns_chestionar_id = ?
                """,
                Long.class,
                atribuireId
        )).isZero();

        Map<String, Object> stare = jdbc.queryForMap("""
                SELECT status, completat_la, scor_total_general
                FROM public.raspunsuri_chestionare
                WHERE id = ?
                """,
                atribuireId
        );

        assertThat(stare.get("status")).isEqualTo("NECOMPLETAT");
        assertThat(stare.get("completat_la")).isNull();
        assertThat(stare.get("scor_total_general")).isNull();
    }

    @Test
    void formularulValidSalveazaRaspunsulFinalizeazaChestionarulSiNotificaMedicul()
            throws Exception {

        verificaChestionarNecompletat();

        MockHttpSession sesiune = autentificaPacient();

        mockMvc.perform(
                        post(
                                "/raspunsuri/completare/{id}/salveaza",
                                atribuireId
                        )
                                .session(sesiune)
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param("intrebare_" + intrebareId, "2")
                )
                .andExpect(handler().methodName("salveazaRaspunsuri"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/chestionare/pacient/"
                                + pacientUserId
                                + "/disponibile"
                ))
                .andExpect(flash().attributeExists("success"))
                .andExpect(flash().attributeCount(1));

        // Exact un răspuns salvat pentru atribuirea curentă.
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM public.raspunsuri_intrebari
            WHERE raspuns_chestionar_id = ?
            """,
                Long.class,
                atribuireId
        )).isEqualTo(1L);

        Map<String, Object> raspuns = jdbc.queryForMap("""
            SELECT intrebare_id, categorie_id, scor
            FROM public.raspunsuri_intrebari
            WHERE raspuns_chestionar_id = ?
            """,
                atribuireId
        );

        assertThat(raspuns.get("intrebare_id")).isEqualTo(intrebareId);
        assertThat(raspuns.get("categorie_id")).isEqualTo(categorieId);
        assertThat(raspuns.get("scor")).isEqualTo(2);

        // Statusul, data completării și scorul sunt persistate.
        Map<String, Object> stare = jdbc.queryForMap("""
            SELECT status, completat_la, scor_total_general
            FROM public.raspunsuri_chestionare
            WHERE id = ?
            """,
                atribuireId
        );

        assertThat(stare.get("status")).isEqualTo("COMPLETAT");
        assertThat(stare.get("completat_la")).isNotNull();
        assertThat((BigDecimal) stare.get("scor_total_general"))
                .isEqualByComparingTo("2.00");

        // Rezultatul calculului există și corespunde răspunsului trimis.
        Map<String, Object> scoring = jdbc.queryForMap("""
            SELECT scor_total,
                   numar_total_intrebari,
                   numar_intrebari_raspunse,
                   procentaj_completare
            FROM public.rezultate_scoring
            WHERE raspuns_chestionar_id = ?
            """,
                atribuireId
        );

        assertThat((BigDecimal) scoring.get("scor_total"))
                .isEqualByComparingTo("2.00");
        assertThat(scoring.get("numar_total_intrebari")).isEqualTo(1);
        assertThat(scoring.get("numar_intrebari_raspunse")).isEqualTo(1);
        assertThat((BigDecimal) scoring.get("procentaj_completare"))
                .isEqualByComparingTo("100.00");

        // Medicul alocat primește notificarea pentru acest chestionar.
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM public.notificari
            WHERE utilizator_destinatar_id = ?
              AND link_actiune = ?
            """,
                Long.class,
                medicUserId,
                "/chestionare/medic/vizualizare/" + atribuireId
        )).isEqualTo(1L);
    }

    private MockHttpSession autentificaPacient() throws Exception {
        MvcResult autentificare = mockMvc.perform(
                        post("/process-login")
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param("username", emailPacient)
                                .param("password", PAROLA_TEST)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated()
                        .withUsername(emailPacient)
                        .withRoles("PACIENT"))
                .andExpect(redirectedUrl(
                        "/pacient/dashboard/" + pacientUserId
                ))
                .andReturn();

        MockHttpSession sesiune = (MockHttpSession)
                autentificare.getRequest().getSession(false);

        assertThat(sesiune).isNotNull();
        assertThat(sesiune.getAttribute("userId"))
                .isEqualTo(pacientUserId.toString());
        assertThat(sesiune.getAttribute("userRole"))
                .isEqualTo("PACIENT");

        return sesiune;
    }

    @Test
    void eroareaLaAlDoileaRaspunsAnuleazaTrimiterea() throws Exception {
        UUID aDouaIntrebareId = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO public.intrebari
                (id, categorie_id, text_intrebare,
                 tip_intrebare, obligatorie, ordine)
            VALUES (?, ?, 'Al doilea simptom de test',
                    'SCOR_0_3', true, 2)
            """,
                aDouaIntrebareId,
                categorieId
        );

        verificaChestionarNecompletat();

        MockHttpSession sesiune = autentificaPacient();

        AtomicInteger incercariSalvare = new AtomicInteger();
        AtomicLong raspunsuriScriseInainteDeEroare = new AtomicLong(-1);

        var executieReala = mockingDetails(raspunsuriRepository)
                .getMockCreationSettings()
                .getDefaultAnswer();

        doAnswer(invocare -> {
            RaspunsuriIntrebari raspuns = invocare.getArgument(0);

            // Intervenim numai pentru atribuirea acestui test.
            if (!atribuireId.equals(
                    raspuns.getRaspunsChestionar().getId())) {
                return executieReala.answer(invocare);
            }

            int numarSalvare = incercariSalvare.incrementAndGet();

            if (numarSalvare == 2) {
                throw new IllegalStateException(
                        "Eroare simulată la salvarea celui de-al doilea răspuns"
                );
            }

            // Folosim delegarea către repository-ul real configurată de Spring.
            Object salvat = executieReala.answer(invocare);

            if (numarSalvare == 1) {
                entityManager.flush();

                Number numarRaspunsuri = (Number) entityManager
                        .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM public.raspunsuri_intrebari
                        WHERE raspuns_chestionar_id = :id
                        """)
                        .setParameter("id", atribuireId)
                        .getSingleResult();

                raspunsuriScriseInainteDeEroare.set(
                        numarRaspunsuri.longValue()
                );
            }

            return salvat;
        }).when(raspunsuriRepository)
                .save(any(RaspunsuriIntrebari.class));

        // Capturăm inclusiv eventualele erori HTTP pentru a putea
        // verifica baza după încheierea cererii.
        Throwable eroareHttp = catchThrowable(() ->
                mockMvc.perform(
                                post(
                                        "/raspunsuri/completare/{id}/salveaza",
                                        atribuireId
                                )
                                        .session(sesiune)
                                        .with(csrf())
                                        .contentType(
                                                MediaType.APPLICATION_FORM_URLENCODED
                                        )
                                        .param(
                                                "intrebare_" + intrebareId,
                                                "2"
                                        )
                                        .param(
                                                "intrebare_" + aDouaIntrebareId,
                                                "3"
                                        )
                        )
                        .andExpect(handler().methodName("salveazaRaspunsuri"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl(
                                "/chestionare/completare/" + atribuireId
                        ))
                        .andExpect(flash().attributeExists("error"))
                        .andExpect(rezultat -> {
                            Map<String, Object> mesaje = rezultat.getFlashMap();

                            assertThat(mesaje)
                                    .doesNotContainKeys(
                                            "success",
                                            "eroriValidare"
                                    );
                        })
        );

        // Confirmăm că am testat efectiv o eroare după prima inserare.
        assertThat(incercariSalvare.get())
                .as("Numărul încercărilor de salvare")
                .isEqualTo(2);

        assertThat(raspunsuriScriseInainteDeEroare.get())
                .as("Răspunsuri scrise în tranzacție înainte de eroare")
                .isEqualTo(1L);

        // După cerere, prima inserare trebuie să fie anulată.
        verificaChestionarNecompletat();

        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM public.rezultate_scoring
            WHERE raspuns_chestionar_id = ?
            """,
                Long.class,
                atribuireId
        )).isZero();

        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM public.notificari
            WHERE utilizator_destinatar_id = ?
              AND link_actiune = ?
            """,
                Long.class,
                medicUserId,
                "/chestionare/medic/vizualizare/" + atribuireId
        )).isZero();

        assertThat(eroareHttp)
                .as("Cererea trebuie tratată prin redirecționare cu mesaj de eroare")
                .isNull();
    }

    @Test
    void eroareaLaSalvareaScoruluiAnuleazaIntreagaTrimitere()
            throws Exception {

        verificaChestionarNecompletat();

        MockHttpSession sesiune = autentificaPacient();

        AtomicLong raspunsuriScrise = new AtomicLong(-1);
        AtomicLong scoruriScrise = new AtomicLong(-1);

        var executieReala = mockingDetails(rezultateScoringRepository)
                .getMockCreationSettings()
                .getDefaultAnswer();

        doAnswer(invocare -> {
            RezultatScoring rezultat = invocare.getArgument(0);

            Object salvat = executieReala.answer(invocare);

            if (!atribuireId.equals(
                    rezultat.getRaspunsChestionar().getId())) {
                return salvat;
            }

            // Scriem răspunsul și scorul în DB, fără confirmarea tranzacției.
            entityManager.flush();

            Number numarRaspunsuri = (Number) entityManager
                    .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM public.raspunsuri_intrebari
                        WHERE raspuns_chestionar_id = :id
                        """)
                    .setParameter("id", atribuireId)
                    .getSingleResult();

            Number numarScoruri = (Number) entityManager
                    .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM public.rezultate_scoring
                        WHERE raspuns_chestionar_id = :id
                        """)
                    .setParameter("id", atribuireId)
                    .getSingleResult();

            raspunsuriScrise.set(numarRaspunsuri.longValue());
            scoruriScrise.set(numarScoruri.longValue());

            throw new IllegalStateException(
                    "Eroare simulată după scrierea scorului"
            );
        }).when(rezultateScoringRepository)
                .save(any(RezultatScoring.class));

        Throwable eroareHttp = catchThrowable(() ->
                mockMvc.perform(
                                post(
                                        "/raspunsuri/completare/{id}/salveaza",
                                        atribuireId
                                )
                                        .session(sesiune)
                                        .with(csrf())
                                        .contentType(
                                                MediaType.APPLICATION_FORM_URLENCODED
                                        )
                                        .param("intrebare_" + intrebareId, "2")
                        )
                        .andExpect(handler().methodName("salveazaRaspunsuri"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl(
                                "/chestionare/completare/" + atribuireId
                        ))
                        .andExpect(flash().attributeExists("error"))
                        .andExpect(rezultat -> {
                            Map<String, Object> mesaje =
                                    rezultat.getFlashMap();

                            assertThat(mesaje)
                                    .doesNotContainKeys(
                                            "success",
                                            "eroriValidare"
                                    );
                        })
        );

        // Confirmăm că am ajuns efectiv la scrierea ambelor în tranzacție.
        assertThat(raspunsuriScrise.get())
                .as("Răspunsuri scrise înainte de eroare")
                .isEqualTo(1L);

        assertThat(scoruriScrise.get())
                .as("Scoruri scrise înainte de eroare")
                .isEqualTo(1L);

        // După anulare, nu trebuie să rămână modificările trimiterii.
        verificaChestionarNecompletat();

        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM public.rezultate_scoring
            WHERE raspuns_chestionar_id = ?
            """,
                Long.class,
                atribuireId
        )).isZero();

        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM public.notificari
            WHERE utilizator_destinatar_id = ?
              AND link_actiune = ?
            """,
                Long.class,
                medicUserId,
                "/chestionare/medic/vizualizare/" + atribuireId
        )).isZero();

        assertThat(eroareHttp)
                .as("Cererea trebuie tratată cu mesaj de eroare")
                .isNull();
    }

    @Test
    void retrimitereaChestionaruluiCompletatEsteRefuzataFaraModificari()
            throws Exception {

        MockHttpSession sesiune = autentificaPacient();

        // Prima trimitere trebuie să reușească.
        mockMvc.perform(
                        post(
                                "/raspunsuri/completare/{id}/salveaza",
                                atribuireId
                        )
                                .session(sesiune)
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param("intrebare_" + intrebareId, "2")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/chestionare/pacient/"
                                + pacientUserId
                                + "/disponibile"
                ))
                .andExpect(flash().attributeExists("success"));

        // Păstrăm situația din DB după prima trimitere.
        var chestionarInitial = jdbc.queryForMap("""
            SELECT *
            FROM public.raspunsuri_chestionare
            WHERE id = ?
            """,
                atribuireId
        );

        var raspunsuriInitiale = jdbc.queryForList("""
            SELECT *
            FROM public.raspunsuri_intrebari
            WHERE raspuns_chestionar_id = ?
            ORDER BY id
            """,
                atribuireId
        );

        var scoruriInitiale = jdbc.queryForList("""
            SELECT *
            FROM public.rezultate_scoring
            WHERE raspuns_chestionar_id = ?
            ORDER BY id
            """,
                atribuireId
        );

        var notificariInitiale = jdbc.queryForList("""
            SELECT *
            FROM public.notificari
            WHERE utilizator_destinatar_id = ?
              AND link_actiune = ?
            ORDER BY id
            """,
                medicUserId,
                "/chestionare/medic/vizualizare/" + atribuireId
        );

        var auditInitial = jdbc.queryForList("""
            SELECT *
            FROM public.audit_log
            WHERE resursa_id = ?
              AND tip_resursa = 'CHESTIONAR'
              AND actiune = 'CREARE'
            ORDER BY id
            """,
                atribuireId
        );

        assertThat(chestionarInitial.get("status")).isEqualTo("COMPLETAT");
        assertThat(chestionarInitial.get("completat_la")).isNotNull();

        assertThat(raspunsuriInitiale).hasSize(1);
        assertThat(raspunsuriInitiale.get(0).get("scor")).isEqualTo(2);

        assertThat(scoruriInitiale).hasSize(1);
        assertThat((BigDecimal) scoruriInitiale.get(0).get("scor_total"))
                .isEqualByComparingTo("2.00");

        assertThat(notificariInitiale).hasSize(1);
        assertThat(auditInitial).hasSize(1);

        // Încercăm să schimbăm răspunsul după finalizare.
        mockMvc.perform(
                        post(
                                "/raspunsuri/completare/{id}/salveaza",
                                atribuireId
                        )
                                .session(sesiune)
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param("intrebare_" + intrebareId, "3")
                )
                .andExpect(status().isConflict());

        // Toate înregistrările trebuie să rămână identice.
        assertThat(jdbc.queryForMap("""
            SELECT *
            FROM public.raspunsuri_chestionare
            WHERE id = ?
            """,
                atribuireId
        )).isEqualTo(chestionarInitial);

        assertThat(jdbc.queryForList("""
            SELECT *
            FROM public.raspunsuri_intrebari
            WHERE raspuns_chestionar_id = ?
            ORDER BY id
            """,
                atribuireId
        )).isEqualTo(raspunsuriInitiale);

        assertThat(jdbc.queryForList("""
            SELECT *
            FROM public.rezultate_scoring
            WHERE raspuns_chestionar_id = ?
            ORDER BY id
            """,
                atribuireId
        )).isEqualTo(scoruriInitiale);

        assertThat(jdbc.queryForList("""
            SELECT *
            FROM public.notificari
            WHERE utilizator_destinatar_id = ?
              AND link_actiune = ?
            ORDER BY id
            """,
                medicUserId,
                "/chestionare/medic/vizualizare/" + atribuireId
        )).isEqualTo(notificariInitiale);

        assertThat(jdbc.queryForList("""
            SELECT *
            FROM public.audit_log
            WHERE resursa_id = ?
              AND tip_resursa = 'CHESTIONAR'
              AND actiune = 'CREARE'
            ORDER BY id
            """,
                atribuireId
        )).isEqualTo(auditInitial);
    }
}