package com.patta.api.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * Formato único de erro da API.
 *
 * Padronizar o erro permite que o frontend trate todas as falhas do mesmo jeito.
 */
@Builder // Gera ApiErrorDTO.builder() para criar o erro no handler global
public record ApiErrorDTO(
        int status,        // Código HTTP (400, 401, 404...)
        String mensagem,   // Texto do erro para exibir ao usuário
        Instant timestamp  // Momento em que o erro ocorreu (UTC)
) {}
