package com.patta.api.dto;

import jakarta.validation.constraints.*;

/**
 * Campos do perfil que o usuário pode editar.
 * E-mail e uid não aparecem aqui porque não podem ser alterados.
 */
public record UserUpdateDTO(

        // Novo nome: obrigatório, até 100 caracteres
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        // Novo telefone: obrigatório, até 30 caracteres
        @NotBlank(message = "O telefone é obrigatório.")
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
        String telefone
) {}
