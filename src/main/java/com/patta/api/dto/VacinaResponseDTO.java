package com.patta.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record VacinaResponseDTO(
        String id,
        String animalId,
        String nome,
        LocalDate aplicadaEm,
        LocalDate proximaDoseEm,
        String lote,
        String profissional,
        String observacoes,
        Instant criadoEm
) {}
