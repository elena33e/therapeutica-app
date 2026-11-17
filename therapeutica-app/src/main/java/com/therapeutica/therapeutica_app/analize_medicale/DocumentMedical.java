package com.therapeutica.therapeutica_app.analize_medicale;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documente_medicale")
@Getter
@Setter
public class DocumentMedical implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    private Long version;

    @Transient
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pacient_id", nullable = false)
    private Pacienti pacient;

    @Column(name = "nume_fisier")
    private String numeFisier;

    @Column(name = "cale_fisier_stocare", nullable = false)
    private String caleFisierStocare;

    @Column(name = "data_incarcare")
    private LocalDateTime dataIncarcare = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusDocument status = StatusDocument.INCARCAT;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @PostLoad
    @PostPersist
    protected void markNotNew() {
        this.isNew = false;
    }

    public enum StatusDocument {
        INCARCAT,
        VALIDAT,
        PROCESAT,
        STANDARDIZAT,
        INTERPRETAT,
        FHIR_GENERAT,
        EROARE,
        STERS_DE_PACIENT
    }
}