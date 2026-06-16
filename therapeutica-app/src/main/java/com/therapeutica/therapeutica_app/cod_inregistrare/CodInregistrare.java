package com.therapeutica.therapeutica_app.cod_inregistrare;

import com.therapeutica.therapeutica_app.utilizatori.RoleType;
import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Getter
@Setter
@Table(name = "cod_inregistrare")
public class CodInregistrare {

    public enum StatusCod {
        NEUTILIZAT,
        UTILIZAT
    }

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cod_unic", nullable = false, columnDefinition = "text", unique = true)
    private String codUnic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "status_cod")
    private StatusCod status = StatusCod.NEUTILIZAT;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generat_de", nullable = false)
    private Utilizatori generatDe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atribuit")
    private Utilizatori atribuit;

    @Column(name = "nume_destinatar", columnDefinition = "text")
    private String numeDestinatar;

    @Column(name = "prenume_destinatar", columnDefinition = "text")
    private String prenumeDestinatar;

    @Column(name = "email_destinatar", nullable = false, columnDefinition = "text")
    private String emailDestinatar;

    @Column(name = "cnp_destinatar", columnDefinition = "text")
    private String cnpDestinatar;

    // Coloana nou adăugată
    @Column(name = "telefon_destinatar", columnDefinition = "text")
    private String telefonDestinatar;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_destinatar", nullable = false, columnDefinition = "role_type")
    private RoleType rolDestinatar;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}