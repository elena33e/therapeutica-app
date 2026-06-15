package com.therapeutica.therapeutica_app.resetare_parola;

import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tokenuri_resetare")
public class TokenResetare {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = Utilizatori.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "utilizator_id")
    private Utilizatori utilizator;

    private LocalDateTime dataExpirare;


}
