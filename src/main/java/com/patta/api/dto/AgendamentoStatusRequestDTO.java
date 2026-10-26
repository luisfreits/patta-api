package com.patta.api.dto;

import com.patta.api.model.AgendamentoStatus;
import jakarta.validation.constraints.NotNull;

public record AgendamentoStatusRequestDTO(
        @NotNull(message = "O status é obrigatório.")
        AgendamentoStatus status
) {}
