package com.patta.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAgendamentoRequestDTO(
        @NotBlank(message = "O usuário é obrigatório.")
        @Size(max = 128, message = "O ID do usuário é inválido.")
        String usuarioId,

        @NotNull(message = "Os dados do agendamento são obrigatórios.")
        @Valid
        AgendamentoRequestDTO agendamento
) {}
