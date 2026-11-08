package com.patta.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AgendamentoResponseDTO(
        String id,
        String usuarioId,
        String animalId,
        String servico,
        LocalDate data,
        LocalTime horario,
        LocalDateTime dataSaidaPrevista,
        Integer tempoHospedagemDias,
        boolean permanenciaExcedente,
        String permanenciaExcedenteDescricao,
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
