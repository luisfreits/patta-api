package com.patta.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Documento da coleção "animais" no Firestore.
 *
 * O Firestore precisa de construtor vazio e de getters/setters públicos para
 * converter documentos em objetos. Por isso esta é uma classe comum e não um record.
 */
@Getter
@Setter
@ToString
@Builder               // Permite AnimalModel.builder()...build()
@NoArgsConstructor     // Usado pelo Firestore ao ler documentos
@AllArgsConstructor    // Necessário para o @Builder funcionar junto com o construtor vazio
public class AnimalModel {

    private String id;       // Id do documento, gerado pelo Firestore ao salvar

    // Chave que liga o animal ao dono. Vários animais podem ter o mesmo userId.
    // É este campo que o AnimalRepository filtra em findByUserId.
    private String userId;

    private String nome;
    private String sexo;
    private Integer idade;
    private String animal;   // Espécie (ex.: "cachorro")
    private Boolean castrado;
}
