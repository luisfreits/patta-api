package com.patta.api.service;

import com.patta.api.model.ClinicaConfigModel;
import com.patta.api.repository.ClinicaConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClinicaServiceTest {

    private static final String ADMIN_UID = "admin";
    private static final String PROFISSIONAL_UID = "profissional";
    private static final String USER_UID = "cliente";

    private final ClinicaConfigRepository repository = mock(ClinicaConfigRepository.class);
    private final ClinicaService service = new ClinicaService(
            repository, new PapelUsuarioService(ADMIN_UID, PROFISSIONAL_UID));

    @Test
    void leStatusDiretamenteDoRepositorio() throws Exception {
        when(repository.buscar()).thenReturn(ClinicaConfigModel.builder().aberta(false).build());

        assertFalse(service.status().aberta());
        assertFalse(service.status().aberta());

        verify(repository, org.mockito.Mockito.times(2)).buscar();
    }

    @Test
    void profissionalPodeFecharEAdministradorPodeAbrir() throws Exception {
        when(repository.alterarEstado(PROFISSIONAL_UID, false))
                .thenReturn(ClinicaConfigModel.builder().aberta(false).build());
        when(repository.alterarEstado(ADMIN_UID, true))
                .thenReturn(ClinicaConfigModel.builder().aberta(true).build());

        assertFalse(service.fechar(PROFISSIONAL_UID).aberta());
        assertTrue(service.abrir(ADMIN_UID).aberta());
    }

    @Test
    void retorna409QuandoEstadoSolicitadoJaEstaConfigurado() throws Exception {
        when(repository.alterarEstado(PROFISSIONAL_UID, false))
                .thenThrow(new ClinicaConfigRepository.EstadoClinicaInalteradoException());
        when(repository.alterarEstado(PROFISSIONAL_UID, true))
                .thenThrow(new ClinicaConfigRepository.EstadoClinicaInalteradoException());

        ResponseStatusException fechada = assertThrows(ResponseStatusException.class,
                () -> service.fechar(PROFISSIONAL_UID));
        ResponseStatusException aberta = assertThrows(ResponseStatusException.class,
                () -> service.abrir(PROFISSIONAL_UID));

        assertEquals(HttpStatus.CONFLICT, fechada.getStatusCode());
        assertEquals("A clínica já está fechada.", fechada.getReason());
        assertEquals(HttpStatus.CONFLICT, aberta.getStatusCode());
        assertEquals("A clínica já está aberta.", aberta.getReason());
    }

    @Test
    void usuarioNaoPodeAbrirNemFechar() {
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.abrir(USER_UID)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.fechar(USER_UID)).getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void bloqueiaCriacaoDeClienteComMensagemContratualQuandoFechada() throws Exception {
        when(repository.buscar()).thenReturn(ClinicaConfigModel.builder().aberta(false).build());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.exigirAbertaParaCliente(USER_UID));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals("A clínica está fechada no momento. Por favor, aguarde.", exception.getReason());
    }

    @Test
    void permiteCriacaoDaEquipeSemLerFlagDaClinica() throws Exception {
        service.exigirAbertaParaCliente(PROFISSIONAL_UID);
        service.exigirAbertaParaCliente(ADMIN_UID);

        verifyNoInteractions(repository);
    }

    @Test
    void consultaEAtualizaCapacidadeSomenteParaEquipe() throws Exception {
        when(repository.buscar()).thenReturn(ClinicaConfigModel.builder()
                .aberta(false)
                .capacidadeHospedagem(4)
                .build());
        when(repository.atualizarCapacidadeHospedagem(6)).thenReturn(ClinicaConfigModel.builder()
                .aberta(false)
                .capacidadeHospedagem(6)
                .build());

        assertEquals(4, service.capacidadeHospedagem(PROFISSIONAL_UID).capacidadeMaxima());
        assertEquals(6, service.configurarCapacidadeHospedagem(ADMIN_UID, 6).capacidadeMaxima());
    }

    @Test
    void rejeitaCapacidadeNaoPositiva() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.configurarCapacidadeHospedagem(ADMIN_UID, 0));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
