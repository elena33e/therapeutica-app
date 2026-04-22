package com.therapeutica.therapeutica_app.notificari;

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

    // ID-ul utilizatorului care trebuie să primească notificarea (ex: Medicul sau Pacientul)
    @Column(name = "utilizator_destinatar_id", nullable = false)
    private UUID utilizatorDestinatarId;

    @Column(nullable = false, length = 100)
    private String titlu;

    @Column(nullable = false, length = 500)
    private String mesaj;

    // Calea relativă în aplicație unde va fi redirecționat utilizatorul la click
    // Ex: "/analize/vizualizeaza/ebf6de2d-de11-47d5-9804-cb7456c98eec"
    @Column(name = "link_actiune", length = 255)
    private String linkActiune;

    // Starea notificării; implicit este falsă la creare
    @Column(nullable = false)
    @Builder.Default
    private boolean citita = false;

    @Column(name = "data_creare", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dataCreare = LocalDateTime.now();
}