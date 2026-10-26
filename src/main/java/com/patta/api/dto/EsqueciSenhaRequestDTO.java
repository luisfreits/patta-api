package com.patta.api.dto;

import jakarta.validation.constraints.*;

/** E-mail para o qual será enviado o link de redefinição de senha. */
public record EsqueciSenhaRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email
) {}
