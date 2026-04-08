package com.therapeutica.therapeutica_app.raport_chestionar;

import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "rezultate_scoring")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RezultatScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raspuns_chestionar_id", nullable = false, unique = true)
    private RaspunsuriChestionare raspunsChestionar;

    @Column(name = "scor_total", nullable = false, precision = 5, scale = 2)
    private BigDecimal scorTotal;


    @Column(name = "culoare_total_hex", nullable = false, length = 7)
    private String culoareTotalHex;

    @Column(name = "culoare_total_nume", nullable = false, length = 20)
    private String culoareTotalNume;

    @Column(name = "interpretare_total", nullable = false, length = 100)
    private String interpretareTotal;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scoruri_categorii", columnDefinition = "jsonb")
    private Map<String, BigDecimal> scoruriCategorii;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "culori_categorii", columnDefinition = "jsonb")
    private Map<String, String> culoriCategorii;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interpretari_categorii", columnDefinition = "jsonb")
    private Map<String, String> interpretariCategorii;

    @Column(name = "numar_total_intrebari", nullable = false)
    private Integer numarTotalIntrebari;

    @Column(name = "numar_intrebari_raspunse", nullable = false)
    private Integer numarIntrebariRaspunse;

    @Column(name = "procentaj_completare", nullable = false, precision = 5, scale = 2)
    private BigDecimal procentajCompletare;

    @Column(name = "versiune_formula", length = 20)
    @Builder.Default
    private String versiuneFormula = "v1.0";

    @Column(name = "calculat_la", nullable = false)
    private LocalDateTime calculatLa;

    @Column(name = "necesita_recalculare")
    @Builder.Default
    private Boolean necesitaRecalculare = false;

    public BigDecimal getScorTotalNormalizat() {
        return scorTotal != null ?
                scorTotal.multiply(BigDecimal.valueOf(20)).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
    }

    // Și o metodă pentru formatare:
    public String getScorTotalDisplay() {
        return scorTotal != null ?
                String.format("%.2f/5 (%.0f%%)",
                        scorTotal.doubleValue(),
                        getScorTotalNormalizat().doubleValue()) :
                "N/A";
    }
}