package com.patta.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Ponto de entrada da API Patta.
 *
 * A autenticação é feita pelo FirebaseTokenFilter, então o usuário em memória
 * que o Spring Boot cria por padrão (e imprime a senha no log) é desligado.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PattaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PattaApplication.class, args);
    }
}
