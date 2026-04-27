package com.therapeutica.therapeutica_app.raspunsuri_chestionare;


import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "raspunsuri_chestionare")
@Getter
@Setter
public class RaspunsuriChestionare {

    public enum StatusRaspuns {
        NECOMPLETAT,    // Atribuit dar necompletat
        IN_PROGRESS,    // În curs de completare
        COMPLETAT,      // Completat de pacient
        REVIZUIT        // Revizuit de medic
    }

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pacient_id", foreignKey = @ForeignKey(name = "raspunsuri_chestionare_pacient_id_fkey"))
    private Pacienti pacient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medic_id", foreignKey = @ForeignKey(name = "raspunsuri_chestionare_medic_id_fkey"))
    private Medici medic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chestionar_id", foreignKey = @ForeignKey(name = "raspunsuri_chestionare_chestionar_id_fkey"))
    private Chestionare chestionar;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private StatusRaspuns status = StatusRaspuns.NECOMPLETAT;

    @Column(name = "completat_la")
    private LocalDateTime completatLa;

    @Column(name = "scor_total_general", precision = 5, scale = 2)
    private BigDecimal scorTotalGeneral;

    @OneToMany(mappedBy = "raspunsChestionar", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RaspunsuriIntrebari> raspunsuri = new ArrayList<>();

    // Constructor
    public RaspunsuriChestionare() {}
}
