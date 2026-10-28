package com.patta.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoResponseDTO(
        String id,
        String usuarioId,
        String animalId,
        String servico,
        LocalDate data,
        LocalTime horario,
        String status,
        String observacoes,
        Instant criadoEm,
        Instant atualizadoEm
) {}
