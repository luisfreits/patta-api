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
        assertEquals("ROLE_USER", service.buscarAutoridades("usuario-comum").getFirst().getAuthority());
    }
}
