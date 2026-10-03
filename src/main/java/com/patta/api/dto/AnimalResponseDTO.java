package com.patta.api.dto;

import lombok.Builder;
import lombok.Data;

// O que a API DEVOLVE. Tem o id (o front precisa para editar/apagar),
// mas não expõe o userId.
@Data
@Builder
public class AnimalResponseDTO {
    private String id;
    private String nome;
    private String sexo;
    private Integer idade;
    private String animal;
    private Boolean castrado;
}