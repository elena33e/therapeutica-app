package com.therapeutica.therapeutica_app.raspunsuri_intrebari;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import com.therapeutica.therapeutica_app.intrebari.IntrebariRepository;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidareRaspunsuriServiceTest {

    @Mock
    private IntrebariRepository intrebariRepository;

    @Mock
    private RaspunsuriChestionareRepository chestionareRepository;

    @Mock
    private Validator validator;

    private ValidareRaspunsuriService service;
    private UUID atribuireId;
    private String cheieIntrebare;
    private Intrebare intrebare;

    @BeforeEach
    void pregatesteDatele() {
        service = new ValidareRaspunsuriService(
                intrebariRepository,
                chestionareRepository,
                new ObjectMapper(),
                validator
        );

        atribuireId = UUID.randomUUID();

        var chestionar = new Chestionare();
        chestionar.setId(UUID.randomUUID());

        var categorie = new CategoriiChestionare();
        categorie.setChestionar(chestionar);

        intrebare = new Intrebare();
        intrebare.setId(UUID.randomUUID());
        intrebare.setCategorie(categorie);
        intrebare.setTextIntrebare("Cât de des apare simptomul?");
        intrebare.setTipIntrebare(Intrebare.TipIntrebare.SCOR_0_3);
        intrebare.setObligatorie(true);

        cheieIntrebare = "intrebare_" + intrebare.getId();

        var atribuire = new RaspunsuriChestionare();
        atribuire.setId(atribuireId);
        atribuire.setChestionar(chestionar);
        atribuire.setPacient(new Pacienti());

        when(chestionareRepository.findByIdForCompletare(atribuireId))
                .thenReturn(Optional.of(atribuire));

        when(intrebariRepository.findByChestionarId(chestionar.getId()))
                .thenReturn(List.of(intrebare));
    }

    @Test
    void respingeRaspunsulObligatoriuLipsa() {
        var date = new LinkedMultiValueMap<String, String>();

        var rezultat = service.valideaza(atribuireId, date);

        assertTrue(rezultat.erori().containsKey(cheieIntrebare));
        assertTrue(rezultat.raspunsuri().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void respingeRaspunsulObligatoriuGol(String valoare) {
        var date = new LinkedMultiValueMap<String, String>();
        date.add(cheieIntrebare, valoare);

        var rezultat = service.valideaza(atribuireId, date);

        assertTrue(rezultat.erori().containsKey(cheieIntrebare));
        assertTrue(rezultat.raspunsuri().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3"})
    void acceptaScorurilePermise(String valoare) {
        var date = new LinkedMultiValueMap<String, String>();
        date.add(cheieIntrebare, valoare);

        var rezultat = service.valideaza(atribuireId, date);

        assertTrue(rezultat.erori().isEmpty());
        assertEquals(valoare, rezultat.raspunsuri().get(cheieIntrebare));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "4", "5", "100", "1.5", "abc"})
    void respingeScorurileNepermise(String valoare) {
        var date = new LinkedMultiValueMap<String, String>();
        date.add(cheieIntrebare, valoare);

        var rezultat = service.valideaza(atribuireId, date);

        assertTrue(rezultat.erori().containsKey(cheieIntrebare));
        assertFalse(rezultat.raspunsuri().containsKey(cheieIntrebare));
    }

    @Test
    void acceptaLipsaUnuiRaspunsOptional() {
        intrebare.setObligatorie(false);
        var date = new LinkedMultiValueMap<String, String>();

        var rezultat = service.valideaza(atribuireId, date);

        assertTrue(rezultat.erori().isEmpty());
        assertTrue(rezultat.raspunsuri().isEmpty());
    }
}