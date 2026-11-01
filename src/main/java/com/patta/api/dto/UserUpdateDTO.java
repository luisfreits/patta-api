package com.patta.api.dto;

import jakarta.validation.constraints.*;

/**
 * Campos do próprio perfil que o usuário autenticado pode editar.
 */
public record UserUpdateDTO(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "O telefone é obrigatório.")
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
        String telefone,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        @Size(max = 128, message = "A senha atual deve ter no máximo 128 caracteres.")
        String senhaAtual
) {
    @Override
    public String toString() {
        return "UserUpdateDTO[nome=" + nome + ", telefone=" + telefone
                + ", email=" + email + ", senhaAtual=***]";
    }
}
