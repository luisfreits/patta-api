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
        LocalDate dataSaidaPrevista,
        Integer duracaoMinutos,
        String status,
        String observacoes,
        Instant checkInEm,
        String checkInPor,
        Instant checkOutEm,
        String checkOutPor,
        Instant criadoEm,
        Instant atualizadoEm
) {}
