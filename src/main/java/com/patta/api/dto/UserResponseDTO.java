package com.patta.api.dto;

import lombok.Builder;

/**
 * Dados do usuário devolvidos pela API. Nunca inclui a senha.
 */
@Builder // Gera UserResponseDTO.builder()
public record UserResponseDTO(
        String id,        // uid do Firebase
        String nome,
        String email,
        String telefone
) {}
