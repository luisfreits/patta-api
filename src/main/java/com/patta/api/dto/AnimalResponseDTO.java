package com.patta.api.dto;

import lombok.Builder;

/**
 * Resposta pública de um animal.
 *
 * Traz o id para o frontend poder operar sobre o animal,
 * mas não expõe o uid do dono.
 */
@Builder // Gera AnimalResponseDTO.builder() para montar o objeto no service
public record AnimalResponseDTO(
        String id,       // Id gerado pelo banco
        String nome,
        String sexo,
        Integer idade,
        String animal,   // Espécie
        Boolean castrado
) {}
