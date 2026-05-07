package com.therapeutica.therapeutica_app.analize_medicale;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documente_medicale")
@Getter
@Setter
public class DocumentMedical implements Persistable<UUID> {

    @Id
    // @GeneratedValue // ELIMINĂ ACEASTA dacă setezi UUID-ul manual în Service
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version // CRUCIAL pentru a rezolva OptimisticLockException
    private Long version;

    @Transient
    private boolean isNew = true; // Flag pentru Spring Data JPA

    @Column(name = "pacient_id", nullable = false)
    private UUID pacientId;

    @Column(name = "nume_fisier")
    private String numeFisier;

    @Column(name = "cale_fisier_stocare", nullable = false)
    private String caleFisierStocare;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "date_brute", columnDefinition = "jsonb")
    private String dateBrute;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "date_validate", columnDefinition = "jsonb")
    private String dateValidate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "date_standardizate_loinc", columnDefinition = "jsonb")
    private String dateStandardizate;

    @Column(name = "data_incarcare")
    private LocalDateTime dataIncarcare = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusDocument status = StatusDocument.INCARCAT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "date_interpretate_fhir", columnDefinition = "jsonb")
    private String dateInterpretate;

    // --- LOGICA PERSISTABLE ---

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

    // --- ENUM ---
    public enum StatusDocument {
        INCARCAT, VALIDAT, PROCESAT, STANDARDIZAT, INTERPRETAT, EROARE, STERS_DE_PACIENT
    }
}