package com.therapeutica.therapeutica_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @Value("${app.features.inference}")
    private boolean inferenceEnabled;

    @ModelAttribute("inferenceEnabled")
    public boolean inferenceEnabled() {
        return inferenceEnabled;
    }
}