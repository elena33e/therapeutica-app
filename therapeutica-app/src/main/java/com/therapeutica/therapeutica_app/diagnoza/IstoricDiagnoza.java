package com.therapeutica.therapeutica_app.diagnoza;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "istoric_diagnoze")
@Getter
@Setter
@NoArgsConstructor
public class IstoricDiagnoza {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pacient_id", nullable = false)
    private Pacienti pacient;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rezultat_json", columnDefinition = "jsonb", nullable = false)
    private String rezultatJson;

    @Column(name = "data_rulare", nullable = false)
    private LocalDateTime dataRulare;
}