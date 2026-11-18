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
}