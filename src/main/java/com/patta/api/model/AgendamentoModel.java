package com.patta.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoModel {

    private String id;
    private String usuarioId;
    private String animalId;
    private String servico;
    private String data;
    private String horario;
    private String dataSaidaPrevista;
    private Integer duracaoMinutos;
    private String status;
    private String observacoes;
    private Date checkInEm;
    private String checkInPor;
    private Date checkOutEm;
    private String checkOutPor;
    private Date criadoEm;
    private Date atualizadoEm;
}
