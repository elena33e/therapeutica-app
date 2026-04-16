package com.therapeutica.therapeutica_app.chestionare;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import com.therapeutica.therapeutica_app.categorii_chestionare.*;

@Entity
@Table(name = "chestionare", schema = "public")
@Getter
@Setter
public class Chestionare {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nume", nullable = false, length = 100)
    private String nume;

    @Column(name = "descriere", columnDefinition = "TEXT")
    private String descriere;

    @Column(name = "instructiuni", columnDefinition = "TEXT")
    private String instructiuni;

    @CreationTimestamp
    @Column(name = "creat_la", nullable = false, updatable = false)
    private LocalDateTime creatLa;

    @OneToMany(mappedBy = "chestionar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoriiChestionare> categoriiChestionare = new ArrayList<>();

    public Chestionare() {
    }

    // Constructor cu parametri (opțional, dar util)
    public Chestionare(String nume, String descriere, String instructiuni) {
        this.nume = nume;
        this.descriere = descriere;
        this.instructiuni = instructiuni;
    }

    // Helper method pentru adăugare categorie
    public void addCategorie(CategoriiChestionare categorie) {
        categoriiChestionare.add(categorie);
        categorie.setChestionar(this);
    }
}