package com.therapeutica.therapeutica_app.medici;
import com.therapeutica.therapeutica_app.pacienti.Pacienti;

import com.therapeutica.therapeutica_app.utilizatori.Utilizatori;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.util.Set;

@Entity
@Getter
@Setter
public class Medici {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Utilizatori user;

    @Column(nullable = false, columnDefinition = "text")
    private String specializare;

    @OneToMany(mappedBy = "medic", fetch = FetchType.LAZY)
    private Set<Pacienti> pacienti;
}
