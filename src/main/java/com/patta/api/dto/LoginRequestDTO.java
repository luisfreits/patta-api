package com.patta.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// LoginRequestDTO.java
/** Credenciais recebidas apenas para autenticação no Firebase; não são persistidas. */
@Data
public class LoginRequestDTO {
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    // Só max, sem min: no login não faz sentido reclamar de senha curta, o Firebase que diz se está errada
    @Size(max = 128, message = "A senha deve ter no máximo 128 caracteres.")
    private String senha;
}