package com.patta.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioFuncionamentoModel {

    private String id;
    private String servico;
    private String diaSemana;
    private java.util.List<IntervaloHorarioModel> intervalos;
    private Integer duracaoMinutos;
    private Integer capacidade;
}
