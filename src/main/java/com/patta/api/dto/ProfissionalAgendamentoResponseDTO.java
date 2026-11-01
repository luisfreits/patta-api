package com.patta.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ProfissionalAgendamentoResponseDTO(
        String id,
        String tutorNome,
        String petNome,
        LocalDate data,
        LocalTime horario,
        boolean hospedagem,
        boolean checkInDisponivel,
        boolean checkOutDisponivel,
        boolean atendimentoDisponivel
) {}
