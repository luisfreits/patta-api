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
public class RegistroClinicoModel {

    private String id;
    private String usuarioId;
    private String animalId;
    private Date dataAtendimento;
    private String profissional;
    private String queixa;
    private String diagnostico;
    private String tratamento;
    private String observacoes;
    private String registradoPor;
    private Date criadoEm;
    private Date atualizadoEm;
}
