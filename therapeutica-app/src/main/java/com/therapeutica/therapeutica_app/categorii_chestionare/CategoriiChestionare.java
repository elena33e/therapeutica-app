package com.therapeutica.therapeutica_app.categorii_chestionare;

import com.therapeutica.therapeutica_app.chestionare.Chestionare;
import com.therapeutica.therapeutica_app.intrebari.Intrebare;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categorii_chestionare", schema = "public")
@Getter
@Setter
public class CategoriiChestionare {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", columnDefinition = "UUID", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "chestionar_id",
            foreignKey = @ForeignKey(name = "categorii_chestionare_chestionar_id_fkey")
    )
    private Chestionare chestionar;

    @Column(name = "nume", nullable = false, length = 100)
    private String nume;

    @Column(name = "descriere", length = 500)
    private String descriere;

    @Column(name = "ordine")
    private Integer ordine;

    @OneToMany(mappedBy = "categorie", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Intrebare> intrebari = new ArrayList<>();

    @Column(name = "este_evaluabila", nullable = false)
    private boolean esteEvaluabila = true;


    @Enumerated(EnumType.STRING)
    @Column(name = "sex_tinta", nullable = false)
    private SexTinta sexTinta = SexTinta.AMBELE;

    public CategoriiChestionare() {}

    public CategoriiChestionare(String nume, Chestionare chestionar) {
        this.nume = nume;
        this.chestionar = chestionar;
    }

    // Metode helper
    public void addIntrebare(Intrebare intrebare) {
        intrebari.add(intrebare);
        intrebare.setCategorie(this);
    }

    public void removeIntrebare(Intrebare intrebare) {
        intrebari.remove(intrebare);
        intrebare.setCategorie(null);
    }
}