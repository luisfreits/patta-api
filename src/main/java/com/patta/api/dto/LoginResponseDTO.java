package com.patta.api.dto;

import lombok.Builder;

/**
 * Dados da sessão e do perfil autenticado devolvidos após o login no Firebase.
 */
@Builder // Gera LoginResponseDTO.builder() para montar a resposta no service
public record LoginResponseDTO(

        // Token que o frontend envia no header Authorization; é o que o filtro valida
        String idToken,

        // Usado para obter um novo idToken quando o atual expira
        String refreshToken,

        // Tempo de vida do idToken em segundos. O Firebase envia como texto e o service converte para Long
        Long expiresIn,

        boolean emailVerified,

        String role
) {

    /**
     * Oculta os tokens no toString para que nunca apareçam em logs.
     */
    @Override
    public String toString() {
        return "LoginResponseDTO[idToken=***, refreshToken=***, expiresIn=" + expiresIn
                + ", emailVerified=" + emailVerified + ", role=" + role + "]";
    }
}
