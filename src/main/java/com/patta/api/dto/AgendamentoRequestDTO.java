package com.patta.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record AgendamentoRequestDTO(
        @Size(max = 128, message = "O ID do animal é inválido.")
        String animalId,

        @Size(max = 20, message = "É permitido solicitar serviços para no máximo 20 pets.")
        List<@NotBlank(message = "O ID do animal é obrigatório.")
        @Size(max = 128, message = "O ID do animal é inválido.") String> animalIds,

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
        this(animalId, null, servico, data, horario, observacoes, null);
    }

    public AgendamentoRequestDTO(String animalId, String servico, LocalDate data,
                                 LocalTime horario, String observacoes, Integer tempoHospedagemDias) {
        this(animalId, null, servico, data, horario, observacoes, tempoHospedagemDias);
    }

    public AgendamentoRequestDTO(List<String> animalIds, String servico, LocalDate data,
                                 LocalTime horario, String observacoes) {
        this(null, animalIds, servico, data, horario, observacoes, null);
    }

    public AgendamentoRequestDTO(List<String> animalIds, String servico, LocalDate data,
                                 LocalTime horario, String observacoes, Integer tempoHospedagemDias) {
        this(null, animalIds, servico, data, horario, observacoes, tempoHospedagemDias);
    }

    @AssertTrue(message = "Informe animalId ou uma lista de animalIds com pelo menos um pet, sem duplicatas.")
    public boolean isAnimaisValidos() {
        boolean animalInformado = animalId != null && !animalId.isBlank();
        boolean listaInformada = animalIds != null && !animalIds.isEmpty();
        if (animalInformado == listaInformada) {
            return false;
        }
        if (animalInformado) {
            return animalId.length() <= 128;
        }
        Set<String> idsUnicos = new HashSet<>();
        return animalIds.size() <= 20 && animalIds.stream()
                .allMatch(id -> id != null && !id.isBlank() && id.length() <= 128 && idsUnicos.add(id.trim()));
    }

    public List<String> idsAnimais() {
        return animalIds == null ? List.of(animalId.trim()) : animalIds.stream().map(String::trim).toList();
    }

    @AssertTrue(message = "Informe tempoHospedagemDias somente para hospedagem e com valor maior que zero.")
    public boolean isHospedagemValida() {
        boolean hospedagem = servico != null && "hospedagem".equals(servico.trim().toLowerCase(Locale.ROOT));
        return hospedagem ? tempoHospedagemDias != null && tempoHospedagemDias >= 1
                : tempoHospedagemDias == null;
    }
}
