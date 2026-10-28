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
    private String status;
    private String observacoes;
    private Date criadoEm;
    private Date atualizadoEm;
}
