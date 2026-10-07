package com.patta.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

// ApiErrorDTO.java
/** Formato único de erro para que os clientes possam tratar falhas previsivelmente. */
@Data
@Builder
public class ApiErrorDTO {
    private int status;       // Código HTTP (400, 401, 404...)
    private String mensagem;  // Texto do erro pra mostrar ao usuário
    private Instant timestamp; // Quando o erro aconteceu
}