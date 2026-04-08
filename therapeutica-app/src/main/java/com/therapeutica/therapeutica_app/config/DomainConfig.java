package com.therapeutica.therapeutica_app.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@EntityScan("com.therapeutica.therapeutica_app")
@EnableJpaRepositories("com.therapeutica.therapeutica_app")
@EnableTransactionManagement
public class DomainConfig {
}
