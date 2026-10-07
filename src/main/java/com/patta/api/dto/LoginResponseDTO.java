package com.patta.api.dto;

import lombok.Builder;
import lombok.Data;

// LoginResponseDTO.java
/** Tokens de sessão devolvidos pelo endpoint de login do Firebase. */
@Data
@Builder
public class LoginResponseDTO {
    private String idToken;      // O token que o front manda no header Authorization (o que o filtro valida)
    private String refreshToken; // Serve pra pegar um idToken novo quando o atual expira
    private String expiresIn;    // Em quantos segundos o idToken expira (o Firebase devolve como texto)
}