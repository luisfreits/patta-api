package com.patta.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadeResponseDTO(
        String servico,
        LocalDate data,
        Integer duracaoMinutos,
        List<HorarioSlotDTO> horarios,
        Integer capacidadeHospedagem,
        Integer vagasDisponiveis
) {
    public DisponibilidadeResponseDTO(String servico, LocalDate data, Integer duracaoMinutos,
                                      List<HorarioSlotDTO> horarios) {
        this(servico, data, duracaoMinutos, horarios, null, null);
    }
}
