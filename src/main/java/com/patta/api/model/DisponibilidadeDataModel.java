package com.patta.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadeDataModel {

    private String id;
    private String servico;
    private String data;
    private List<IntervaloHorarioModel> intervalos;
    private Integer duracaoMinutos;
    private Integer capacidade;
}
