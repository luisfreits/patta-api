package com.patta.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados enviados pelo formulário público de contato. */
public record ContatoRequestDTO(

        @NotBlank(message = "Informe seu nome.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "Informe o assunto.")
        @Size(max = 120, message = "O assunto deve ter no máximo 120 caracteres.")
        String assunto,

        @NotBlank(message = "Escreva sua mensagem.")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2.000 caracteres.")
        String mensagem
) {}
