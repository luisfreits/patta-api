package com.patta.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CapacidadeHospedagemRequestDTO(
        @NotNull(message = "A capacidade máxima é obrigatória.")
        @Positive(message = "A capacidade máxima deve ser maior que zero.")
        Integer capacidadeMaxima
) {}
