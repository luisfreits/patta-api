package com.patta.api.dto;

import lombok.Data;

@Data
public class CadastroRequestDTO {
    private String nome;
    private String email;
    private String telefone;
    private String senha;
    private String confirmaSenha;
}