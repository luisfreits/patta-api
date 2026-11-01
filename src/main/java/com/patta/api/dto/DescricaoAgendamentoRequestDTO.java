package com.patta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DescricaoAgendamentoRequestDTO(
        @NotBlank(message = "A descrição do problema é obrigatória.")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao
) {}
