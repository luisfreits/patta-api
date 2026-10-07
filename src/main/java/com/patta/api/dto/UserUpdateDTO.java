package com.patta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// UserUpdateDTO.java
/** Campos editáveis do perfil; o e-mail e o uid permanecem imutáveis. */
@Data
public class UserUpdateDTO {
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotBlank(message = "O telefone é obrigatório.")
    @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
    private String telefone;
}