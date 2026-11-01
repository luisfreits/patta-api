package com.patta.api.dto;

import java.time.Instant;

public record RegistroClinicoResponseDTO(
        String id,
        String animalId,
        Instant dataAtendimento,
        String profissional,
        String queixa,
        String diagnostico,
        String tratamento,
        String observacoes,
        Instant criadoEm,
        Instant atualizadoEm
) {}
