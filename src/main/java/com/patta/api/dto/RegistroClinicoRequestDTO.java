package com.patta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RegistroClinicoRequestDTO(
        @NotNull(message = "A data do atendimento é obrigatória.")
        Instant dataAtendimento,
        @NotBlank(message = "O profissional é obrigatório.")
        @Size(max = 160, message = "O nome do profissional deve ter no máximo 160 caracteres.")
        String profissional,
        @NotBlank(message = "A queixa é obrigatória.")
        @Size(max = 2000, message = "A queixa deve ter no máximo 2000 caracteres.")
        String queixa,
        @NotBlank(message = "O diagnóstico é obrigatório.")
        @Size(max = 2000, message = "O diagnóstico deve ter no máximo 2000 caracteres.")
        String diagnostico,
        @NotBlank(message = "O tratamento é obrigatório.")
        @Size(max = 2000, message = "O tratamento deve ter no máximo 2000 caracteres.")
        String tratamento,
        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String observacoes
) {}
