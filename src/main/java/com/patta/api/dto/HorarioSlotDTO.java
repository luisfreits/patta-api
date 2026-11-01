package com.patta.api.dto;

import java.time.LocalTime;

public record HorarioSlotDTO(
        LocalTime horario,
        int capacidade,
        int vagasDisponiveis
) {}
