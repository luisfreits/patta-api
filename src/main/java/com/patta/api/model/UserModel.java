package com.patta.api.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {

    // O id é o UID do Firebase Authentication e também users/{uid} no Firestore.
    private String id;

    private String nome;
    private String email;
    private String telefone;
}
