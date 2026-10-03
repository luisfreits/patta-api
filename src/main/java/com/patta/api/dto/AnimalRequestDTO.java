package com.patta.api.dto;

import lombok.Data;

// O que o cliente ENVIA ao criar/editar.
// Sem "id" e sem "userId": o id é gerado pelo banco e o userId vem
// da identificação do usuário logado, nunca do JSON (senão alguém
// poderia criar animais "em nome" de outro usuário).
@Data
public class AnimalRequestDTO {
    private String nome;
    private String sexo;
    private Integer idade;
    private String animal;
    private Boolean castrado;
}