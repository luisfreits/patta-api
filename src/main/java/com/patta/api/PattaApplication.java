package com.patta.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// A autenticação da API é feita pelo filtro Firebase, não pelo usuário em memória padrão do Boot.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PattaApplication {

	public static void main(String[] args) {
		SpringApplication.run(PattaApplication.class, args);
	}

}
