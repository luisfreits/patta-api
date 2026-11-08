package com.patta.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record HorarioFuncionamentoRequestDTO(
        @NotEmpty(message = "Informe ao menos um intervalo de funcionamento.")
        List<@Valid IntervaloHorarioDTO> intervalos,
        @NotNull(message = "A duração do serviço é obrigatória.")
        @Min(value = 5, message = "A duração mínima é de 5 minutos.")
        @Max(value = 1440, message = "A duração máxima é de 1440 minutos.")
        Integer duracaoMinutos,
        @NotNull(message = "A capacidade é obrigatória.")
        @Min(value = 1, message = "A capacidade deve ser maior que zero.")
        @Max(value = 100, message = "A capacidade máxima é 100.")
        Integer capacidade,
        boolean continuarHorarioPadrao
) {
    public HorarioFuncionamentoRequestDTO(
            List<@Valid IntervaloHorarioDTO> intervalos, Integer duracaoMinutos, Integer capacidade) {
        this(intervalos, duracaoMinutos, capacidade, false);
    }

    public record IntervaloHorarioDTO(
            @NotNull(message = "O início do intervalo é obrigatório.")
            LocalTime inicio,
            @NotNull(message = "O fim do intervalo é obrigatório.")
            LocalTime fim
    ) {}
}
