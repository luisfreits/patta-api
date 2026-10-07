package com.patta.api.dto;

import lombok.Builder;
import lombok.Data;

// UserResponseDTO.java
/** Dados seguros de usuário devolvidos pela API, nunca incluindo senha. */
@Data
@Builder
public class UserResponseDTO {
    private String id; // É o uid do Firebase
    private String nome;
    private String email;
    private String telefone;
}