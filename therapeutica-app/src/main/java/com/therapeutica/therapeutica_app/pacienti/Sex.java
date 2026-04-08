package com.therapeutica.therapeutica_app.pacienti;

import lombok.Getter;

@Getter
public enum Sex {
    MASCULIN("Masculin"),
    FEMININ("Feminin"),
    NEPRECIZAT("Neprecizat");

    private final String numeAfisat;

    Sex(String numeAfisat) {
        this.numeAfisat = numeAfisat;
    }
}
