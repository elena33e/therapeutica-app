package com.therapeutica.therapeutica_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
@EnableAsync
public class TherapeuticaAppApplication {

    public static void main(final String[] args) {
        SpringApplication.run(TherapeuticaAppApplication.class, args);
    }

    @Bean
    public RestTemplate ocrRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Timeout de conectare
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(300000);

        return new RestTemplate(factory);
    }
}