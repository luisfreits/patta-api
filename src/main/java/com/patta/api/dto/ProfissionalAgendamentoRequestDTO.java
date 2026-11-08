package com.patta.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

public record ProfissionalAgendamentoRequestDTO(
        @NotBlank(message = "O serviço é obrigatório.")
        String servico,
        @NotNull(message = "A data é obrigatória.")
        @FutureOrPresent(message = "A data não pode estar no passado.")
        LocalDate data,
        @NotNull(message = "O horário de entrada é obrigatório.")
        LocalTime horario,
        @Min(value = 1, message = "O tempo de hospedagem deve ser de pelo menos 1 dia.")
        Integer tempoHospedagemDias
) {
    @AssertTrue(message = "Informe tempoHospedagemDias somente para hospedagem e com valor maior que zero.")
    public boolean isHospedagemValida() {
        boolean hospedagem = servico != null && "hospedagem".equals(servico.trim().toLowerCase(Locale.ROOT));
        return hospedagem ? tempoHospedagemDias != null && tempoHospedagemDias >= 1
                : tempoHospedagemDias == null;
    }
}
