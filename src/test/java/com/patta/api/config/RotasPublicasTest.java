package com.patta.api.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotasPublicasTest {

    @Test
    void loginECadastroContinuamPublicos() {
        assertTrue(RotasPublicas.isPublica("/auth/login"));
        assertTrue(RotasPublicas.isPublica("/cadastro/solicitar"));
    }

    @Test
    void verificacaoExigeTokenMasPermiteContaAindaNaoVerificada() {
        assertFalse(RotasPublicas.isPublica(RotasPublicas.ROTA_REENVIO_VERIFICACAO));
        assertFalse(RotasPublicas.isPublica(RotasPublicas.ROTA_STATUS_VERIFICACAO));
        assertTrue(RotasPublicas.permiteContaNaoVerificada(RotasPublicas.ROTA_REENVIO_VERIFICACAO));
        assertTrue(RotasPublicas.permiteContaNaoVerificada(RotasPublicas.ROTA_STATUS_VERIFICACAO));
        assertFalse(RotasPublicas.permiteContaNaoVerificada("/users/me"));
    }

    @Test
    void perfilDePapelExigeAutenticacao() {
        assertFalse(RotasPublicas.isPublica(RotasPublicas.ROTA_PERFIL_AUTENTICADO));
        assertFalse(RotasPublicas.permiteContaNaoVerificada(RotasPublicas.ROTA_PERFIL_AUTENTICADO));
    }
}
