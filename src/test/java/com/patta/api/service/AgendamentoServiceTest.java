package com.patta.api.service;

import com.patta.api.dto.AgendamentoRequestDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.AgendamentoStatusRequestDTO;
import com.patta.api.dto.AnimalResponseDTO;
import com.patta.api.model.AgendamentoModel;
import com.patta.api.model.AgendamentoStatus;
import com.patta.api.repository.AgendamentoRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgendamentoServiceTest {

    private static final String UID = "client-uid";
    private static final String OTHER_UID = "other-uid";
    private static final String ADMIN_UID = "admin-uid";
    private static final ValidatorFactory VALIDATION_FACTORY = Validation.buildDefaultValidatorFactory();

    private final AgendamentoRepository repository = mock(AgendamentoRepository.class);
    private final AnimalService animalService = mock(AnimalService.class);
    private final AgendamentoService service = new AgendamentoService(repository, animalService, ADMIN_UID);

    @BeforeEach
    void limparMocks() {
        reset(repository, animalService);
    }

    @AfterAll
    static void fecharValidationFactory() throws Exception {
        VALIDATION_FACTORY.close();
    }

    @Test
    void criaAgendamentoComIdentidadeEStatusDefinidosNoServidor() throws Exception {
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(repository.save(any(AgendamentoModel.class))).thenAnswer(invocation -> {
            AgendamentoModel model = invocation.getArgument(0);
            model.setId("appointment-1");
            return model;
        });

        AgendamentoResponseDTO response = service.criar(UID, request("animal-1", "CONSULTA"));

        assertEquals("appointment-1", response.id());
        assertEquals(UID, response.usuarioId());
        assertEquals("PENDENTE", response.status());
        assertEquals("consulta", response.servico());
        verify(animalService).buscar(UID, "animal-1");
    }

    @Test
    void listaUsandoUidAutenticadoComoUnicoFiltro() throws Exception {
        when(repository.findByUsuarioId(UID)).thenReturn(List.of(model("a1", UID, "PENDENTE")));

        assertEquals(1, service.listar(UID).size());
        verify(repository).findByUsuarioId(UID);
    }

    @Test
    void clienteNaoPodeConsultarAgendamentoDeOutraPessoa() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", OTHER_UID, "PENDENTE")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.buscar(UID, "a1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void administradorPodeConsultarAgendamentoDeOutroUsuario() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", OTHER_UID, "PENDENTE")));

        assertEquals(OTHER_UID, service.buscar(ADMIN_UID, "a1").usuarioId());
    }

    @Test
    void animalDeOutroUsuarioImpedeCriarAgendamento() throws Exception {
        when(animalService.buscar(UID, "animal-foreign"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado."));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criar(UID, request("animal-foreign", "consulta")));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaServicoDesconhecido() throws Exception {
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criar(UID, request("animal-1", "banho")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void dtoRejeitaCamposAusentesEDataPassada() {
        AgendamentoRequestDTO invalid = new AgendamentoRequestDTO(
                "", "consulta", LocalDate.now().minusDays(1), null, "x".repeat(1001));

        assertFalse(VALIDATION_FACTORY.getValidator().validate(invalid).isEmpty());
    }

    @Test
    void usuarioComumNaoPodeAlterarStatusAdministrativamente() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.alterarStatus(UID, "a1",
                        new AgendamentoStatusRequestDTO(AgendamentoStatus.APROVADO)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void semUidsAdministrativosConfiguradosNinguemPodeAlterarStatus() {
        AgendamentoService serviceSemAdministradores = new AgendamentoService(repository, animalService, "");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceSemAdministradores.alterarStatus(ADMIN_UID, "a1",
                        new AgendamentoStatusRequestDTO(AgendamentoStatus.APROVADO)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void administradorPodeAprovarAgendamentoPendente() throws Exception {
        AgendamentoModel pending = model("a1", UID, "PENDENTE");
        when(repository.findById("a1")).thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        AgendamentoResponseDTO response = service.alterarStatus(ADMIN_UID, "a1",
                new AgendamentoStatusRequestDTO(AgendamentoStatus.APROVADO));

        assertEquals("APROVADO", response.status());
        verify(repository).save(pending);
    }

    @Test
    void administradorPodeRejeitarAgendamentoPendente() throws Exception {
        AgendamentoModel pending = model("a1", UID, "PENDENTE");
        when(repository.findById("a1")).thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        AgendamentoResponseDTO response = service.alterarStatus(ADMIN_UID, "a1",
                new AgendamentoStatusRequestDTO(AgendamentoStatus.REJEITADO));

        assertEquals("REJEITADO", response.status());
    }

    @Test
    void rejeitaTransicaoDeStatusInvalida() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", UID, "APROVADO")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.alterarStatus(ADMIN_UID, "a1",
                        new AgendamentoStatusRequestDTO(AgendamentoStatus.REJEITADO)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void agendamentoInexistenteRetorna404() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.buscar(UID, "missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private AgendamentoRequestDTO request(String animalId, String servico) {
        return new AgendamentoRequestDTO(
                animalId, servico, LocalDate.now().plusDays(2), LocalTime.of(10, 30), "Consulta de rotina");
    }

    private AgendamentoModel model(String id, String usuarioId, String status) {
        Date now = new Date();
        return AgendamentoModel.builder()
                .id(id)
                .usuarioId(usuarioId)
                .animalId("animal-1")
                .servico("consulta")
                .data(LocalDate.now().plusDays(1).toString())
                .horario("10:00")
                .status(status)
                .observacoes(null)
                .criadoEm(now)
                .atualizadoEm(now)
                .build();
    }

    private AnimalResponseDTO animalResponse(String id) {
        return AnimalResponseDTO.builder().id(id).nome("Rex").build();
    }
}
