package com.patta.api.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// CadastroRequestDTO.java
@Data
public class CadastroRequestDTO {
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.") // Confere o formato (algo@dominio)
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.") // 254 é o limite padrão de e-mail
    private String email;

    @NotBlank(message = "O telefone é obrigatório.")
    @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
    private String telefone;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 128, message = "A senha deve ter entre 6 e 128 caracteres.") // 6 é o mínimo do Firebase
    private String senha;

    @NotBlank(message = "A confirmação da senha é obrigatória.")
    private String confirmaSenha; // Só confere se veio. Comparar com "senha" é no service
}