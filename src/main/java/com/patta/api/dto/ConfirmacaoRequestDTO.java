package com.patta.api.dto;

import lombok.Data;

@Data
public class ConfirmacaoRequestDTO {
    private String email;
    private String codigo;
}