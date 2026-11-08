package com.patta.api.service;

import com.patta.api.dto.AgendamentoRequestDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.AgendamentoStatusRequestDTO;
import com.patta.api.dto.AnimalResponseDTO;
import com.patta.api.dto.ProfissionalAgendamentoRequestDTO;
import com.patta.api.model.AgendamentoModel;
import com.patta.api.model.AgendamentoStatus;
import com.patta.api.model.ClinicaConfigModel;
import com.patta.api.model.HorarioFuncionamentoModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.ClinicaConfigRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class AgendamentoServiceTest {

    private static final String UID = "client-uid";
    private static final String OTHER_UID = "other-uid";
    private static final String ADMIN_UID = "admin-uid";
    private static final ValidatorFactory VALIDATION_FACTORY = Validation.buildDefaultValidatorFactory();

    private final AgendamentoRepository repository = mock(AgendamentoRepository.class);
    private final AnimalService animalService = mock(AnimalService.class);
    private final DisponibilidadeService disponibilidadeService = mock(DisponibilidadeService.class);
    private final PapelUsuarioService papelUsuarioService = new PapelUsuarioService(ADMIN_UID, "vet-uid");
    private final ClinicaService clinicaService = mock(ClinicaService.class);
    private final AgendamentoService service = new AgendamentoService(
            repository, animalService, papelUsuarioService, disponibilidadeService, clinicaService);

    @BeforeEach
    void limparMocks() {
        reset(repository, animalService, disponibilidadeService);
    }

    @AfterAll
    static void fecharValidationFactory() throws Exception {
        VALIDATION_FACTORY.close();
    }

    @Test
    void criaAgendamentoComIdentidadeEStatusDefinidosNoServidor() throws Exception {
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(disponibilidadeService.validarSlot(anyString(), any(), any())).thenReturn(disponibilidade());
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt())).thenAnswer(invocation -> {
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
    void bloqueiaCriacaoDeClienteQuandoClinicaEstaFechada() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                ClinicaService.MENSAGEM_CLINICA_FECHADA))
                .when(clinicaService).exigirAbertaParaCliente(UID);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criar(UID, request("animal-1", "consulta")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals("A clínica está fechada no momento. Por favor, aguarde.", exception.getReason());
        verifyNoInteractions(animalService, repository);
    }

    @Test
    void profissionalPodeCriarEnquantoClinicaEstaFechada() throws Exception {
        ClinicaConfigRepository clinicaRepository = mock(ClinicaConfigRepository.class);
        ClinicaService clinicaFechada = new ClinicaService(clinicaRepository, papelUsuarioService);
        AgendamentoService serviceComClinicaFechada = new AgendamentoService(
                repository, animalService, papelUsuarioService, disponibilidadeService, clinicaFechada);
        when(clinicaRepository.buscar()).thenReturn(ClinicaConfigModel.builder().aberta(false).build());
        when(animalService.buscar("vet-uid", "animal-1")).thenReturn(animalResponse("animal-1"));
        when(disponibilidadeService.validarSlot(anyString(), any(), any())).thenReturn(disponibilidade());
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt())).thenAnswer(invocation -> {
            AgendamentoModel model = invocation.getArgument(0);
            model.setId("appointment-professional");
            return model;
        });

        AgendamentoResponseDTO response = serviceComClinicaFechada.criar(
                "vet-uid", request("animal-1", "consulta"));

        assertEquals("appointment-professional", response.id());
        verifyNoInteractions(clinicaRepository);
    }

    @Test
    void calculaHospedagemDeCincoDiasComoCentoEVinteHorasSemGradeHoraria() throws Exception {
        LocalDate entrada = LocalDate.now().plusDays(2);
        AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                "animal-1", "hospedagem", entrada, LocalTime.of(10, 30), null, 5);
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt())).thenAnswer(invocation -> {
            AgendamentoModel model = invocation.getArgument(0);
            model.setId("boarding-5-days");
            return model;
        });

        AgendamentoResponseDTO response = service.criar(UID, request);

        assertEquals(5, response.tempoHospedagemDias());
        assertEquals(LocalDateTime.of(entrada, LocalTime.of(10, 30)).plusHours(120),
                response.dataSaidaPrevista());
        assertNull(response.duracaoMinutos());
        verify(disponibilidadeService, org.mockito.Mockito.never()).validarSlot(anyString(), any(), any());
    }

    @Test
    void hospedagemComEntradaHojeIgnoraHorarioOcultoMeiaNoite() throws Exception {
        AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                "animal-1", "hospedagem", LocalDate.now(), LocalTime.MIDNIGHT, null, 1);
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoResponseDTO response = service.criar(UID, request);

        assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).plusHours(24),
                response.dataSaidaPrevista());
    }

    @Test
    void hospedagemSemVagaRetorna409ComMensagemExata() throws Exception {
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt()))
                .thenThrow(new AgendamentoRepository.ConflitoReservaException());
        AgendamentoRequestDTO request = new AgendamentoRequestDTO(
                "animal-1", "hospedagem", LocalDate.now().plusDays(2), LocalTime.NOON, null, 2);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criar(UID, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Não há vagas disponíveis para hospedagem no período solicitado.", exception.getReason());
    }

    @Test
    void criaAgendamentoProfissionalUsandoTutorEPetDaReferenciaVisivel() throws Exception {
        AgendamentoModel referencia = model("referencia", UID, "APROVADO");
        when(repository.findById("referencia")).thenReturn(Optional.of(referencia));
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(disponibilidadeService.validarSlot(anyString(), any(), any())).thenReturn(disponibilidade());
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt())).thenAnswer(invocation -> {
            AgendamentoModel novo = invocation.getArgument(0);
            novo.setId("profissional-novo");
            return novo;
        });
        ProfissionalAgendamentoRequestDTO request = new ProfissionalAgendamentoRequestDTO(
                "consulta", LocalDate.now().plusDays(2), LocalTime.of(11, 0), null);

        AgendamentoResponseDTO response = service.criarParaProfissional("vet-uid", "referencia", request);

        assertEquals(UID, response.usuarioId());
        assertEquals("animal-1", response.animalId());
        verify(animalService).buscar(UID, "animal-1");
    }

    @Test
    void profissionalNaoPodeCriarAgendamentoComReferenciaDeOutroPapel() throws Exception {
        when(repository.findById("referencia")).thenReturn(Optional.of(model("referencia", ADMIN_UID, "APROVADO")));
        ProfissionalAgendamentoRequestDTO request = new ProfissionalAgendamentoRequestDTO(
                "consulta", LocalDate.now().plusDays(2), LocalTime.of(11, 0), null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criarParaProfissional("vet-uid", "referencia", request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(animalService);
        verify(repository, org.mockito.Mockito.never()).saveIfAvailable(any(), anyInt());
    }

    @Test
    void impedeAgendamentoDuplicadoParaMesmoPetServicoDataEHorario() throws Exception {
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(disponibilidadeService.validarSlot(anyString(), any(), any())).thenReturn(disponibilidade());
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt()))
                .thenThrow(new AgendamentoRepository.AgendamentoDuplicadoException());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criar(UID, request("animal-1", "consulta")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Este pet já possui um agendamento ativo para o mesmo serviço, data e horário.",
                exception.getReason());
    }

    @Test
    void administradorPodeCriarAgendamentoParaPetDoCliente() throws Exception {
        when(animalService.buscar(UID, "animal-1")).thenReturn(animalResponse("animal-1"));
        when(disponibilidadeService.validarSlot(anyString(), any(), any())).thenReturn(disponibilidade());
        when(repository.saveIfAvailable(any(AgendamentoModel.class), anyInt())).thenAnswer(invocation -> {
            AgendamentoModel model = invocation.getArgument(0);
            model.setId("appointment-admin");
            return model;
        });

        AgendamentoResponseDTO response = service.criarParaUsuario(ADMIN_UID, UID, request("animal-1", "consulta"));

        assertEquals(UID, response.usuarioId());
        assertEquals("appointment-admin", response.id());
        verify(animalService).buscar(UID, "animal-1");
    }

    @Test
    void usuarioComumNaoPodeCriarAgendamentoParaOutraPessoa() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criarParaUsuario(UID, OTHER_UID, request("animal-1", "consulta")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(animalService, repository);
    }

    @Test
    void listaUsandoUidAutenticadoComoUnicoFiltro() throws Exception {
        when(repository.findByUsuarioId(UID)).thenReturn(List.of(model("a1", UID, "PENDENTE")));

        assertEquals(1, service.listar(UID).size());
        verify(repository).findByUsuarioId(UID);
    }

    @Test
    void administradorListaAgendamentosDeTodosOsUsuarios() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                model("a1", UID, "PENDENTE"),
                model("a2", OTHER_UID, "PENDENTE")));

        List<AgendamentoResponseDTO> response = service.listar(ADMIN_UID);

        assertEquals(2, response.size());
        assertEquals(List.of(UID, OTHER_UID), response.stream().map(AgendamentoResponseDTO::usuarioId).toList());
        verify(repository).findAll();
    }

    @Test
    void clienteNaoPodeConsultarAgendamentoDeOutraPessoa() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", OTHER_UID, "PENDENTE")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.buscar(UID, "a1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void profissionalNaoPodeConsultarAgendamentoCompletoDeOutroUsuario() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", OTHER_UID, "PENDENTE")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.buscar("vet-uid", "a1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void profissionalPodeConsultarInternamenteAgendamentoParaEquipe() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", OTHER_UID, "PENDENTE")));

        assertEquals(OTHER_UID, service.buscarParaEquipe("vet-uid", "a1").usuarioId());
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
    void dtoExigeDiasSomenteParaHospedagem() {
        AgendamentoRequestDTO semDias = new AgendamentoRequestDTO(
                "animal-1", "hospedagem", LocalDate.now().plusDays(1), LocalTime.NOON, null, null);
        AgendamentoRequestDTO diasEmConsulta = new AgendamentoRequestDTO(
                "animal-1", "consulta", LocalDate.now().plusDays(1), LocalTime.NOON, null, 2);

        assertFalse(VALIDATION_FACTORY.getValidator().validate(semDias).isEmpty());
        assertFalse(VALIDATION_FACTORY.getValidator().validate(diasEmConsulta).isEmpty());
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
        AgendamentoService serviceSemAdministradores = new AgendamentoService(
                repository, animalService, new PapelUsuarioService(""), disponibilidadeService, clinicaService);

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
    void profissionalPodeAprovarAgendamentoPendenteDeCliente() throws Exception {
        AgendamentoModel pending = model("a1", UID, "PENDENTE");
        when(repository.findById("a1")).thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        AgendamentoResponseDTO response = service.alterarStatus("vet-uid", "a1",
                new AgendamentoStatusRequestDTO(AgendamentoStatus.APROVADO));

        assertEquals("APROVADO", response.status());
        verify(repository).save(pending);
    }

    @Test
    void administradorRegistraCheckInUmaUnicaVez() throws Exception {
        AgendamentoModel approved = model("a1", UID, "APROVADO");
        when(repository.findById("a1")).thenReturn(Optional.of(approved));
        when(repository.save(approved)).thenReturn(approved);

        AgendamentoResponseDTO response = service.marcarCheckIn(ADMIN_UID, "a1");

        assertEquals(UID, response.usuarioId());
        assertFalse(response.checkInEm() == null);
        assertEquals(ADMIN_UID, response.checkInPor());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.marcarCheckIn(ADMIN_UID, "a1")).getStatusCode());
    }

    @Test
    void checkOutRegistraHospedagemDepoisDoCheckIn() throws Exception {
        AgendamentoModel boarding = model("a1", UID, "APROVADO");
        boarding.setServico("hospedagem");
        boarding.setCheckInEm(new Date());
        when(repository.findById("a1")).thenReturn(Optional.of(boarding));
        when(repository.save(boarding)).thenReturn(boarding);

        AgendamentoResponseDTO response = service.marcarCheckOut(ADMIN_UID, "a1");

        assertFalse(response.checkOutEm() == null);
    }

    @Test
    void tutorPodeCancelarAgendamentoProprioSemConsultarEstadoDaClinica() throws Exception {
        AgendamentoModel appointment = model("a1", UID, "PENDENTE");
        when(repository.findById("a1")).thenReturn(Optional.of(appointment));
        when(repository.save(appointment)).thenReturn(appointment);

        AgendamentoResponseDTO response = service.cancelar(UID, "a1");

        assertEquals("CANCELADO", response.status());
        verify(repository).save(appointment);
        verifyNoInteractions(clinicaService);
    }

    @Test
    void tutorNaoPodeCancelarAgendamentoDeOutraPessoa() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", OTHER_UID, "PENDENTE")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.cancelar(UID, "a1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void naoPermiteCancelarAgendamentoRejeitadoCanceladoOuComCheckIn() throws Exception {
        AgendamentoModel rejected = model("rejected", UID, "REJEITADO");
        AgendamentoModel canceled = model("canceled", UID, "CANCELADO");
        AgendamentoModel checkedIn = model("checked-in", UID, "APROVADO");
        checkedIn.setCheckInEm(new Date());
        when(repository.findById("rejected")).thenReturn(Optional.of(rejected));
        when(repository.findById("canceled")).thenReturn(Optional.of(canceled));
        when(repository.findById("checked-in")).thenReturn(Optional.of(checkedIn));

        for (String id : List.of("rejected", "canceled", "checked-in")) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> service.cancelar(UID, id));
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void tutorPodeCorrigirDescricaoDoProprioAgendamento() throws Exception {
        AgendamentoModel appointment = model("a1", UID, "PENDENTE");
        when(repository.findById("a1")).thenReturn(Optional.of(appointment));
        when(repository.save(appointment)).thenReturn(appointment);

        AgendamentoResponseDTO response = service.alterarDescricao(UID, "a1", "Texto corrigido");

        assertEquals("Texto corrigido", response.observacoes());
    }

    @Test
    void tutorNaoPodeCorrigirDescricaoDeOutroUsuario() throws Exception {
        when(repository.findById("a1")).thenReturn(Optional.of(model("a1", OTHER_UID, "PENDENTE")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.alterarDescricao(UID, "a1", "Texto"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
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

    @Test
    void leHospedagemLegadaEInfereDuracaoPelaDiferencaDeDatas() throws Exception {
        LocalDate entrada = LocalDate.now().minusDays(10);
        AgendamentoModel legado = model("legado", UID, "APROVADO");
        legado.setServico("hospedagem");
        legado.setData(entrada.toString());
        legado.setHorario("10:30");
        legado.setDataSaidaPrevista(entrada.plusDays(4).toString());
        when(repository.findById("legado")).thenReturn(Optional.of(legado));

        AgendamentoResponseDTO response = service.buscar(UID, "legado");

        assertEquals(4, response.tempoHospedagemDias());
        assertEquals(LocalDateTime.of(entrada.plusDays(4), LocalTime.of(10, 30)),
                response.dataSaidaPrevista());
    }

    @Test
    void formataOsTresIntervalosDePermanenciaExcedente() throws Exception {
        assertEquals("Permanência além do contratado: 30 min",
                responseComExcedente(1, Duration.ofMinutes(30)).permanenciaExcedenteDescricao());
        assertEquals("Permanência além do contratado: 2 h 15 min",
                responseComExcedente(1, Duration.ofHours(2).plusMinutes(15))
                        .permanenciaExcedenteDescricao());
        assertEquals("Permanência além do contratado: 1 dia 3 h",
                responseComExcedente(2, Duration.ofDays(1).plusHours(3))
                        .permanenciaExcedenteDescricao());
    }

    @Test
    void naoMostraExcedenteDepoisDoCheckOut() throws Exception {
        AgendamentoModel hospedagem = model("encerrada", UID, "APROVADO");
        hospedagem.setServico("hospedagem");
        hospedagem.setTempoHospedagemDias(1);
        hospedagem.setDataSaidaPrevista(LocalDateTime.now().plusDays(1).toString());
        hospedagem.setCheckInEm(Date.from(Instant.now().minus(Duration.ofDays(3))));
        hospedagem.setCheckOutEm(new Date());
        when(repository.findById("encerrada")).thenReturn(Optional.of(hospedagem));

        AgendamentoResponseDTO response = service.buscar(UID, "encerrada");

        assertFalse(response.permanenciaExcedente());
        assertNull(response.permanenciaExcedenteDescricao());
    }

    private AgendamentoResponseDTO responseComExcedente(int diasContratados, Duration excedente) throws Exception {
        Instant agora = Instant.now();
        AgendamentoModel hospedagem = model("excedente", UID, "APROVADO");
        hospedagem.setServico("hospedagem");
        hospedagem.setTempoHospedagemDias(diasContratados);
        hospedagem.setDataSaidaPrevista(LocalDateTime.of(
                LocalDate.now().plusDays(2), LocalTime.NOON).toString());
        hospedagem.setCheckInEm(Date.from(agora.minus(Duration.ofDays(diasContratados)).minus(excedente)));
        when(repository.findById("excedente")).thenReturn(Optional.of(hospedagem));
        return service.buscar(UID, "excedente");
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

    private HorarioFuncionamentoModel disponibilidade() {
        return HorarioFuncionamentoModel.builder()
                .duracaoMinutos(30)
                .capacidade(1)
                .build();
    }
}
