package com.patta.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ProfissionalAgendamentoResponseDTO(
        String id,
        String tutorNome,
        String petNome,
        LocalDate data,
        LocalTime horario,
        LocalDateTime dataSaidaPrevista,
        Integer tempoHospedagemDias,
        String status,
        boolean permanenciaExcedente,
        String permanenciaExcedenteDescricao,
        boolean hospedagem,
        boolean checkInDisponivel,
        boolean checkOutDisponivel,
        boolean atendimentoDisponivel
) {}
