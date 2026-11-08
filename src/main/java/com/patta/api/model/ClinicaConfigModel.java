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
public class ClinicaConfigModel {

    private Boolean aberta;
    private String atualizadoPor;
    private Date atualizadoEm;
    private Integer capacidadeHospedagem;
}
