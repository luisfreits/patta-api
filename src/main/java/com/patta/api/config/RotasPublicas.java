package com.patta.api.config;

import java.util.List;
import java.util.Arrays;

/**
 * Prefixos das rotas que não exigem token.
 *
 * Fica num único lugar para que o filtro de token e as regras do Spring Security
 * nunca fiquem diferentes entre si.
 */
public final class RotasPublicas {

    private static final List<String> PREFIXOS = List.of("/auth", "/cadastro");
    public static final String ROTA_REENVIO_VERIFICACAO = "/auth/verificacao/reenviar";
    public static final String ROTA_STATUS_VERIFICACAO = "/auth/verificacao/status";
    public static final String ROTA_PERFIL_AUTENTICADO = "/auth/me";
    public static final String[] ROTAS_AUTENTICADAS = {
            ROTA_REENVIO_VERIFICACAO,
            ROTA_STATUS_VERIFICACAO,
            ROTA_PERFIL_AUTENTICADO
    };

    /** Padrões no formato do Spring Security, por exemplo "/auth/**". */
    public static final String[] PADROES = PREFIXOS.stream()
            .map(prefixo -> prefixo + "/**")
            .toArray(String[]::new);

    private RotasPublicas() {
        // Classe utilitária: não pode ser instanciada
    }

    /** Retorna true se o caminho é o prefixo exato ou está dentro de um prefixo público. */
    public static boolean isPublica(String caminho) {
        return Arrays.stream(ROTAS_AUTENTICADAS).noneMatch(caminho::equals) && PREFIXOS.stream()
                .anyMatch(prefixo -> caminho.equals(prefixo) || caminho.startsWith(prefixo + "/"));
    }

    /** Rotas autenticadas que precisam funcionar antes da confirmação do e-mail. */
    public static boolean permiteContaNaoVerificada(String caminho) {
        return ROTA_REENVIO_VERIFICACAO.equals(caminho) || ROTA_STATUS_VERIFICACAO.equals(caminho);
    }
}
