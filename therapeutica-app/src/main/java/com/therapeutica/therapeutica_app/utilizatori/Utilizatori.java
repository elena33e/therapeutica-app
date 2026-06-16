package com.therapeutica.therapeutica_app.utilizatori;

import com.therapeutica.therapeutica_app.cod_inregistrare.CodInregistrare;
import com.therapeutica.therapeutica_app.medici.Medici;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Utilizatori {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();


    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String email;

    @Column(nullable = false, columnDefinition = "text")
    private String nume;

    @Column(nullable = false, columnDefinition = "text")
    private String prenume;

    @Column(nullable = false, columnDefinition = "text")
    private String parola;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "role_type")
    private RoleType rol;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Medici medic;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Pacienti pacient;

    @OneToMany(mappedBy = "generatDe")
    private Set<CodInregistrare> coduriGenerate = new HashSet<>();


    @OneToMany(mappedBy = "atribuit")
    private Set<CodInregistrare> coduriAtribuite = new HashSet<>();
}