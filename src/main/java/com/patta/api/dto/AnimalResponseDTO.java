package com.patta.api.dto;

import lombok.Builder;
import lombok.Data;

// AnimalResponseDTO.java
// Resposta pública do animal: permite ao frontend operar pelo id sem revelar o uid do dono.
@Data
@Builder // Lombok cria o .builder() pra montar o objeto de forma legível no service
public class AnimalResponseDTO {
    private String id; // Agora tem id (o banco gerou), mas não tem uid do dono
    private String nome;
    private String sexo;
    private Integer idade;
    private String animal;
    private Boolean castrado;
}