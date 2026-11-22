
package com.patta.api.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Centraliza as rotas públicas e as exceções de autenticação.
 */
public final class RotasPublicas {

    private static final List<String> PREFIXOS = List.of("/auth", "/cadastro");

    public static final String ROTA_CONTATO = "/contato";
    private static final List<String> ROTAS_PUBLICAS_EXATAS = List.of(ROTA_CONTATO);

    public static final String ROTA_REENVIO_VERIFICACAO = "/auth/verificacao/reenviar";
    public static final String ROTA_STATUS_VERIFICACAO = "/auth/verificacao/status";
    public static final String ROTA_PERFIL_AUTENTICADO = "/auth/me";

    public static final String[] ROTAS_AUTENTICADAS = {
        ROTA_REENVIO_VERIFICACAO,
        ROTA_STATUS_VERIFICACAO,
        ROTA_PERFIL_AUTENTICADO
    };

    /** Padrões utilizados pelo Spring Security. */
    public static final String[] PADROES = Stream.concat(
            PREFIXOS.stream().map(p -> p + "/**"),
            ROTAS_PUBLICAS_EXATAS.stream()
    ).toArray(String[]::new);

    private RotasPublicas() {
    }

    /** Verifica se uma rota dispensa autenticação. */
    public static boolean isPublica(String caminho) {
        return !Arrays.asList(ROTAS_AUTENTICADAS).contains(caminho)
                && (ROTAS_PUBLICAS_EXATAS.contains(caminho)
                || PREFIXOS.contains(caminho)
                || PREFIXOS.stream().anyMatch(p -> caminho.startsWith(p + "/")));
    }

    /** Verifica se uma conta não verificada pode acessar a rota. */
    public static boolean permiteContaNaoVerificada(String caminho) {
        return ROTA_REENVIO_VERIFICACAO.equals(caminho)
                || ROTA_STATUS_VERIFICACAO.equals(caminho);
    }
}