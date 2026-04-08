package com.therapeutica.therapeutica_app.intrebari;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "intrebari")
@Getter
@Setter
public class Intrebare {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private CategoriiChestionare categorie;

    @Column(name = "text_intrebare", nullable = false, columnDefinition = "TEXT")
    private String textIntrebare;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_intrebare", length = 20)
    private TipIntrebare tipIntrebare = TipIntrebare.SCOR_0_3;

    @Column(name = "creat_la")
    private LocalDateTime creatLa = LocalDateTime.now();

    // Opțional - poziția în cadrul categoriei
    @Column(name = "ordine")
    private Integer ordine = 0;

    // Opțional - dacă este obligatorie
    @Column(name = "obligatorie")
    private Boolean obligatorie = true;

    // Opțional - pentru întrebări cu opțiuni multiple (JSON)
    @Column(name = "optiuni_json", columnDefinition = "jsonb")
    private String optiuniJson;

    @Column(name = "hpo_code", length = 50)
    private String hpoCode;

    @Column(name = "hpo_term", length = 255)
    private String hpoTerm;

    @Column(name = "hpo_trigger_code")
    private String hpoTriggerCode;

    @Column(name = "hpo_trigger_term")
    private String hpoTriggerTerm;

    public enum TipIntrebare {
        SCOR_0_3,      // Scor 0,1,2,3
        SCOR_1_5,      // Scor 1-5
        DA_NU,         // Da/Nu
        MULTIPLE_CHOICE, // Alege din opțiuni
        TEXT_SCURT,    // Răspuns scurt text
        TEXT_LIBER,    // Răspuns lung text
        NUMERIC,       // Număr
        DATA,          // Dată
        EMAIL,         // Email
        TELEFON        // Telefon
    }
}