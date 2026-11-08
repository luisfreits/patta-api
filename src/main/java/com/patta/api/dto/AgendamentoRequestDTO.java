package com.patta.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

public record AgendamentoRequestDTO(
        @NotBlank(message = "O animal é obrigatório.")
        @Size(max = 128, message = "O ID do animal é inválido.")
        String animalId,

        @NotBlank(message = "O serviço é obrigatório.")
        String servico,

        @NotNull(message = "A data é obrigatória.")
        @FutureOrPresent(message = "A data não pode estar no passado.")
        LocalDate data,

        @NotNull(message = "O horário é obrigatório.")
        LocalTime horario,

        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes,

        @Min(value = 1, message = "O tempo de hospedagem deve ser de pelo menos 1 dia.")
        Integer tempoHospedagemDias
) {
    public AgendamentoRequestDTO(String animalId, String servico, LocalDate data,
                                 LocalTime horario, String observacoes) {
        this(animalId, servico, data, horario, observacoes, null);
    }

    @AssertTrue(message = "Informe tempoHospedagemDias somente para hospedagem e com valor maior que zero.")
    public boolean isHospedagemValida() {
        boolean hospedagem = servico != null && "hospedagem".equals(servico.trim().toLowerCase(Locale.ROOT));
        return hospedagem ? tempoHospedagemDias != null && tempoHospedagemDias >= 1
                : tempoHospedagemDias == null;
    }
}
