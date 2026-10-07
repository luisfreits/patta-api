package com.patta.api.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// ConfirmacaoRequestDTO.java
@Data
public class ConfirmacaoRequestDTO {
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    private String email;

    @NotBlank(message = "O código de confirmação é obrigatório.")
    // regexp: \d{6} = exatamente 6 dígitos. O "\\" é porque em Java a barra precisa ser escrita duas vezes
    @Pattern(regexp = "\\d{6}", message = "O código deve conter exatamente 6 dígitos.")
    private String codigo;
}