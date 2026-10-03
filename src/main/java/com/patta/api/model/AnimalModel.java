package com.patta.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Data gera getters, setters, equals, hashCode e toString automaticamente.
// @Builder permite criar o objeto como você já faz no UserModel (AnimalModel.builder()...build()).
// @NoArgsConstructor: o Firestore EXIGE construtor vazio para ler documentos (toObject).
// @AllArgsConstructor: o @Builder precisa dele para funcionar junto com o construtor vazio.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalModel {

    // ID do documento, gerado pelo Firestore ao salvar.
    private String id;

    // A "chave estrangeira": id do usuário dono do animal.
    // É este campo que liga N animais a 1 usuário.
    private String userId;

    private String nome;
    private String sexo;
    private Integer idade;
    private String animal;     // espécie (ex.: "cachorro")
    private Boolean castrado;
}