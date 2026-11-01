package com.patta.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadeResponseDTO(
        String servico,
        LocalDate data,
        int duracaoMinutos,
        List<HorarioSlotDTO> horarios
) {}
