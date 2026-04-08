package com.therapeutica.therapeutica_app.raspunsuri_intrebari;

import com.therapeutica.therapeutica_app.categorii_chestionare.CategoriiChestionare;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "raspunsuri_intrebari")
@Getter
@Setter
public class RaspunsuriIntrebari {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raspuns_chestionar_id", nullable = false)
    private RaspunsuriChestionare raspunsChestionar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intrebare_id", nullable = false)
    private Intrebare intrebare;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private CategoriiChestionare categorie;

    @Column(name = "scor")
    private Integer scor;

    @Column(name = "raspuns_text", columnDefinition = "TEXT")
    private String raspunsText;

    @Column(name = "raspuns_numeric", precision = 10, scale = 2)
    private BigDecimal raspunsNumeric;

    @Column(name = "raspuns_data")
    private LocalDateTime raspunsData;

    @Column(name = "raspuns_json", columnDefinition = "TEXT")
    private String raspunsJson;

    @Column(name = "creat_la")
    private LocalDateTime creatLa = LocalDateTime.now();

    // Helper methods
    public boolean isScor() {
        return scor != null;
    }

    public boolean isText() {
        return raspunsText != null && !raspunsText.isEmpty();
    }

    public boolean isNumeric() {
        return raspunsNumeric != null;
    }
}