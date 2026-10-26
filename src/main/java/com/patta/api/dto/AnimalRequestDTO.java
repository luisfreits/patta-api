package com.patta.api.dto;

import jakarta.validation.constraints.*;

/**
 * Dados enviados pelo cliente para criar ou editar um animal.
 *
 * Não contém "id" nem "userId": o id é gerado pelo banco e o dono vem
 * do usuário autenticado, nunca do JSON. Assim ninguém cria animais
 * em nome de outro usuário.
 */
public record AnimalRequestDTO(

        // Nome do animal: obrigatório, sem espaços só, até 100 caracteres
        @NotBlank(message = "O nome do animal é obrigatório.")
        @Size(max = 100, message = "O nome do animal deve ter no máximo 100 caracteres.")
        String nome,

        // Sexo do animal: obrigatório, até 30 caracteres
        @NotBlank(message = "O sexo do animal é obrigatório.")
        @Size(max = 30, message = "O sexo deve ter no máximo 30 caracteres.")
        String sexo,

        // Idade: Integer (objeto) para que @NotNull consiga detectar ausência; mínimo 0
        @NotNull(message = "A idade é obrigatória.")
        @Min(value = 0, message = "A idade não pode ser negativa.")
        Integer idade,

        // Espécie (cachorro, gato...): obrigatória, até 60 caracteres
        @NotBlank(message = "A espécie do animal é obrigatória.")
        @Size(max = 60, message = "A espécie deve ter no máximo 60 caracteres.")
        String animal,

        // Castrado: Boolean (objeto) para detectar quando o campo não foi enviado
        @NotNull(message = "Informe se o animal é castrado.")
        Boolean castrado
) {}
