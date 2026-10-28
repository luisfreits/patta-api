package com.patta.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Documento da coleção "users" no Firestore.
 *
 * O id é o uid do Firebase Authentication, então o documento users/{uid}
 * sempre corresponde à conta de login. A senha nunca fica aqui.
 */
@Getter
@Setter
@ToString
@Builder               // Permite UserModel.builder()...build()
@NoArgsConstructor     // Usado pelo Firestore ao ler documentos
@AllArgsConstructor    // Necessário para o @Builder funcionar junto com o construtor vazio
public class UserModel {

    private String id;        // uid do Firebase Authentication, também o id do documento
    private String nome;
    private String email;     // Normalizado em minúsculas no cadastro
    private String telefone;
}
