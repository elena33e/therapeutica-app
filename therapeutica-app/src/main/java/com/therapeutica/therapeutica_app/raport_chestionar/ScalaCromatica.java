package com.therapeutica.therapeutica_app.raport_chestionar;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum ScalaCromatica {

    // Pragurile sunt acum interpretate ca "până la":
    // VERDE (0-1), GALBEN (1-2), PORTOCALIU (2-3), ROSU (3-4), ROSU_INCHIS (4+)
    VERDE(1.0, "#10B981", "VERDE", "Normal", 1),
    GALBEN(2.0, "#FBBF24", "GALBEN", "Ușor afectat", 2),
    PORTOCALIU(3.0, "#F97316", "PORTOCALIU", "Moderat afectat", 3),
    ROSU(4.0, "#EF4444", "ROȘU", "Sever afectat", 4),
    ROSU_INCHIS(Double.MAX_VALUE, "#991B1B", "ROȘU ÎNCHIS", "Critic", 5);

    private final double pragSuperior;
    private final String culoareHex;
    private final String culoareNume;
    private final String interpretare;
    private final int nivelSeveritate;

    ScalaCromatica(double pragSuperior, String culoareHex,
                   String culoareNume, String interpretare, int nivelSeveritate) {
        this.pragSuperior = pragSuperior;
        this.culoareHex = culoareHex;
        this.culoareNume = culoareNume;
        this.interpretare = interpretare;
        this.nivelSeveritate = nivelSeveritate;
    }

    public static ScalaCromatica pentruScor(double scor) {
        // Folosim stream pentru o căutare curată: prima categorie al cărei prag e >= scorul nostru
        return Arrays.stream(values())
                .filter(interval -> scor <= interval.pragSuperior)
                .findFirst()
                .orElse(ROSU_INCHIS);
    }
}