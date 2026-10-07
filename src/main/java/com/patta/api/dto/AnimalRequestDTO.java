package com.patta.api.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// AnimalRequestDTO.java
// O que o cliente ENVIA ao criar/editar.
// Sem "id" e sem "userId": o id é gerado pelo banco e o userId vem
// da identificação do usuário logado, nunca do JSON (senão alguém
// poderia criar animais "em nome" de outro usuário).
@Data // Lombok cria getters, setters, toString, equals e hashCode
public class AnimalRequestDTO {
    @NotBlank(message = "O nome do animal é obrigatório.") // Não aceita null, vazio nem só espaços
    @Size(max = 100, message = "O nome do animal deve ter no máximo 100 caracteres.") // Limita o tamanho
    private String nome;

    @NotBlank(message = "O sexo do animal é obrigatório.")
    @Size(max = 30, message = "O sexo deve ter no máximo 30 caracteres.")
    private String sexo;

    @NotNull(message = "A idade é obrigatória.") // Integer (com I maiúsculo) pode ser null, então dá pra checar se veio
    @Min(value = 0, message = "A idade não pode ser negativa.") // Valor mínimo 0
    private Integer idade;

    @NotBlank(message = "A espécie do animal é obrigatória.")
    @Size(max = 60, message = "A espécie deve ter no máximo 60 caracteres.")
    private String animal; // Aqui "animal" quer dizer a espécie (cachorro, gato...)

    @NotNull(message = "Informe se o animal é castrado.") // Boolean (objeto) pra poder detectar que faltou
    private Boolean castrado;
}