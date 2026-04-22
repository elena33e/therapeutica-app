package com.therapeutica.therapeutica_app.pacienti;

import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Getter
@Setter
public class Pacienti {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Utilizatori user;

    @Column(name = "cnp", unique = true)
    private String cnp;

    @Column(name = "data_nasterii")
    private java.time.LocalDate dataNasterii;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex")
    private Sex sex; // Enum: MASCULIN, FEMININ

    @Column(name = "inaltime")
    private Integer inaltime; // in cm (ex: 180)

    @Column(name = "greutate")
    private Double greutate; // in kg (ex: 85.5)

    @Column(name = "grupa_sangvina")
    private String grupaSangvina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medic_id", nullable = true)
    private Medici medic;

    // Metodă utilitară pentru calculul vârstei (logică de business)
    public int getVarsta() {
        if (this.dataNasterii == null) return 0;
        return java.time.Period.between(this.dataNasterii, java.time.LocalDate.now()).getYears();
    }
}
