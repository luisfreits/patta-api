package com.patta.api.dto;

import jakarta.validation.constraints.*;

/**
 * Credenciais de login. Usadas apenas para autenticar no Firebase e não são persistidas.
 */
public record LoginRequestDTO(

        // E-mail do usuário
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        // Senha: só tem limite máximo. No login não faz sentido reclamar de senha curta,
        // quem decide se a senha está correta é o Firebase
        @NotBlank(message = "A senha é obrigatória.")
        @Size(max = 128, message = "A senha deve ter no máximo 128 caracteres.")
        String senha
) {

    /**
     * Sobrescreve o toString gerado pelo record para ocultar a senha.
     * Sem isso, um log com o objeto imprimiria a senha em texto puro.
     */
    @Override
    public String toString() {
        return "LoginRequestDTO[email=" + email + ", senha=***]";
    }
}
