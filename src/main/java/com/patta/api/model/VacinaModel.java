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
public class VacinaModel {

    private String id;
    private String usuarioId;
    private String animalId;
    private String nome;
    private Date aplicadaEm;
    private Date proximaDoseEm;
    private String lote;
    private String profissional;
    private String observacoes;
    private String registradoPor;
    private Date criadoEm;
    private Date atualizadoEm;
}
