package com.patta.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

public record ProfissionalAgendamentoRequestDTO(
        @Size(max = 20, message = "É permitido solicitar serviços para no máximo 20 pets.")
        List<@NotBlank(message = "O ID do animal é obrigatório.")
        @Size(max = 128, message = "O ID do animal é inválido.") String> animalIds,
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
    public ProfissionalAgendamentoRequestDTO(String servico, LocalDate data, LocalTime horario,
                                             Integer tempoHospedagemDias) {
        this(null, servico, data, horario, tempoHospedagemDias);
    }

    @AssertTrue(message = "Informe tempoHospedagemDias somente para hospedagem e com valor maior que zero.")
    public boolean isHospedagemValida() {
        boolean hospedagem = servico != null && "hospedagem".equals(servico.trim().toLowerCase(Locale.ROOT));
        return hospedagem ? tempoHospedagemDias != null && tempoHospedagemDias >= 1
                : tempoHospedagemDias == null;
    }

    @AssertTrue(message = "A lista de animalIds deve conter pelo menos um pet e não pode ter duplicatas.")
    public boolean isAnimaisValidos() {
        if (animalIds == null) {
            return true;
        }
        return !animalIds.isEmpty() && animalIds.size() <= 20
                && animalIds.stream().allMatch(id -> id != null && !id.isBlank() && id.length() <= 128)
                && animalIds.stream().map(String::trim).distinct().count() == animalIds.size();
    }
}
