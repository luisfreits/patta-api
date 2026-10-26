package com.patta.api.config;

import tools.jackson.databind.ObjectMapper;
import com.patta.api.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Escreve erros no formato padrão da API direto na resposta HTTP.
 * Usado pelos pontos em que o GlobalExceptionHandler não é executado (filtros e Spring Security).
 */
public final class RespostaErro {

    private RespostaErro() {
        // Classe utilitária
    }

    /** Define status, tipo JSON, UTF-8 e escreve o ApiErrorDTO no corpo. */
    public static void escrever(HttpServletResponse response, ObjectMapper objectMapper,
                                int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // Acentos saem corretos
        objectMapper.writeValue(response.getWriter(), ApiErrorDTO.builder()
                .status(status)
                .mensagem(mensagem)
                .timestamp(Instant.now())
                .build());
    }
}
