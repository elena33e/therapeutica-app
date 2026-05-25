package com.therapeutica.therapeutica_app.diagnostic;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diagnostic_medic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticMedic {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pacient_id", nullable = false)
    private Pacienti pacient;

    @Column(nullable = false)
    private String diagnosticFinal;

    @Column(columnDefinition = "TEXT")
    private String observatiiClinice;

    // Hibernate creează automat un tabel pentru aceste UUID-uri
    @ElementCollection
    @CollectionTable(name = "diagnostic_chestionare_ref", joinColumns = @JoinColumn(name = "diagnostic_id"))
    @Column(name = "chestionar_id")
    private List<UUID> chestionareReferinte = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "diagnostic_analize_ref", joinColumns = @JoinColumn(name = "diagnostic_id"))
    @Column(name = "analiza_id")
    private List<UUID> analizeReferinte = new ArrayList<>();

    private LocalDateTime dataStabilire;

    @PrePersist
    protected void onCreate() {
        this.dataStabilire = LocalDateTime.now();
    }
}