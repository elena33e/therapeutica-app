package com.therapeutica.therapeutica_app.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Jurnal de audit pentru accesul la datele cu caracter personal.
 * Append-only: se scrie o singura data, nu se modifica si nu se sterge.
 * Nu contine date medicale, doar referinte (id-uri).
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_pacient", columnList = "pacient_id"),
        @Index(name = "idx_audit_utilizator", columnList = "utilizator_id"),
        @Index(name = "idx_audit_data", columnList = "data_ora")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "utilizator_id")
    private UUID utilizatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actiune", nullable = false)
    private ActiuneAudit actiune;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_resursa", nullable = false)
    private TipResursa tipResursa;

    @Column(name = "resursa_id")
    private UUID resursaId;

    @Column(name = "pacient_id")
    private UUID pacientId;

    @Column(name = "data_ora", nullable = false)
    private LocalDateTime dataOra;

    @Column(name = "adresa_ip", length = 45)
    private String adresaIp;

    @PrePersist
    protected void onCreate() {
        if (this.dataOra == null) {
            this.dataOra = LocalDateTime.now();
        }
    }

    public enum ActiuneAudit {
        VIZUALIZARE, CREARE, MODIFICARE, STERGERE, EXPORT
    }

    public enum TipResursa {
        DOSAR_PACIENT, DOCUMENT_MEDICAL, CHESTIONAR, DIAGNOSTIC, PROFIL_PACIENT
    }
}