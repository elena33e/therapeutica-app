package com.therapeutica.therapeutica_app.notificari;

import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificari")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizator_destinatar_id", nullable = false)
    private Utilizatori utilizatorDestinatar;

    @Column(nullable = false, length = 100)
    private String titlu;

    @Column(nullable = false, length = 500)
    private String mesaj;

    // Calea unde va fi redirecționat utilizatorul la click
    @Column(name = "link_actiune", length = 255)
    private String linkActiune;

    @Column(nullable = false)
    @Builder.Default
    private boolean citita = false;

    @Column(name = "data_creare", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dataCreare = LocalDateTime.now();
}