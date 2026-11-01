package com.patta.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PapelUsuarioServiceTest {

    @Test
    void somenteUidsConfiguradosRecebemPapelAdministrador() {
        PapelUsuarioService service = new PapelUsuarioService(" admin-1,admin-2 , ");

        assertTrue(service.ehAdministrador("admin-1"));
        assertEquals("ADMIN", service.buscarPapel("admin-2"));
        assertFalse(service.ehAdministrador("usuario-comum"));
        assertEquals("USER", service.buscarPapel("usuario-comum"));
        assertEquals("ROLE_ADMIN", service.buscarAutoridades("admin-1").getFirst().getAuthority());
        assertTrue(service.buscarAutoridades("admin-1").stream()
                .anyMatch(autoridade -> autoridade.getAuthority().equals("ROLE_PROFISSIONAL")));
        assertTrue(service.buscarAutoridades("admin-1").stream()
                .anyMatch(autoridade -> autoridade.getAuthority().equals("ROLE_USER")));
        assertFalse(service.buscarAutoridades("usuario-comum").stream()
                .anyMatch(autoridade -> autoridade.getAuthority().equals("ROLE_PROFISSIONAL")));
        assertEquals("ROLE_USER", service.buscarAutoridades("usuario-comum").getFirst().getAuthority());
    }

    @Test
    void profissionaisRecebemAcessoDeAtendimentoSemPapelAdministrativo() {
        PapelUsuarioService service = new PapelUsuarioService("admin-1", "vet-1, vet-2");

        assertTrue(service.ehProfissional("vet-1"));
        assertTrue(service.podeGerenciarAtendimento("vet-1"));
        assertEquals("PROFISSIONAL", service.buscarPapel("vet-2"));
        assertEquals("ROLE_PROFISSIONAL", service.buscarAutoridades("vet-1").getFirst().getAuthority());
        assertTrue(service.buscarAutoridades("vet-1").stream()
                .anyMatch(autoridade -> autoridade.getAuthority().equals("ROLE_USER")));
        assertFalse(service.buscarAutoridades("vet-1").stream()
                .anyMatch(autoridade -> autoridade.getAuthority().equals("ROLE_ADMIN")));
        assertFalse(service.ehAdministrador("vet-1"));
        assertEquals("ADMIN", service.buscarPapel("admin-1"));
    }
}
