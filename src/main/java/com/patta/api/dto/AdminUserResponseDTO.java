package com.patta.api.dto;

public record AdminUserResponseDTO(
        String id,
        String nome,
        String email,
        String telefone,
        String role
) {}
