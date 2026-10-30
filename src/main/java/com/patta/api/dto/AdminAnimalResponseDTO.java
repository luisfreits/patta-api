package com.patta.api.dto;

public record AdminAnimalResponseDTO(
        String id,
        String userId,
        String nome,
        String sexo,
        Integer idade,
        String animal,
        Boolean castrado
) {}
