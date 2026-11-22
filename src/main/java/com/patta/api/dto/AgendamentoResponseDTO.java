package com.patta.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
        Instant atualizadoEm,
        List<String> animalIds
) {
    public AgendamentoResponseDTO(
            String id, String usuarioId, String animalId, String servico, LocalDate data, LocalTime horario,
            LocalDateTime dataSaidaPrevista, Integer tempoHospedagemDias, boolean permanenciaExcedente,
            String permanenciaExcedenteDescricao, Integer duracaoMinutos, String status, String observacoes,
            Instant checkInEm, String checkInPor, Instant checkOutEm, String checkOutPor, Instant criadoEm,
            Instant atualizadoEm) {
        this(id, usuarioId, animalId, servico, data, horario, dataSaidaPrevista, tempoHospedagemDias,
                permanenciaExcedente, permanenciaExcedenteDescricao, duracaoMinutos, status, observacoes,
                checkInEm, checkInPor, checkOutEm, checkOutPor, criadoEm, atualizadoEm,
                animalId == null ? List.of() : List.of(animalId));
    }
}
