package com.patta.api.dto;

/**
 * Mensagem simples de confirmação para operações que não retornam uma entidade.
 * Ex.: "Cadastro realizado. Verifique seu e-mail."
 */
public record MessageResponseDTO(String mensagem) {}
