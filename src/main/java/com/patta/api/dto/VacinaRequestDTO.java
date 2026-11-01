package com.patta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VacinaRequestDTO(
        @NotBlank(message = "O nome da vacina é obrigatório.")
        @Size(max = 160, message = "O nome da vacina deve ter no máximo 160 caracteres.")
        String nome,
        @NotNull(message = "A data da aplicação é obrigatória.")
        LocalDate aplicadaEm,
        LocalDate proximaDoseEm,
        @Size(max = 100, message = "O lote deve ter no máximo 100 caracteres.")
        String lote,
        @NotBlank(message = "O profissional é obrigatório.")
        @Size(max = 160, message = "O nome do profissional deve ter no máximo 160 caracteres.")
        String profissional,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes
) {}
