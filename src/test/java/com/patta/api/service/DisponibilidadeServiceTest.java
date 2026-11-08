package com.patta.api.service;

import com.patta.api.dto.HorarioFuncionamentoRequestDTO;
import com.patta.api.model.AgendamentoModel;
import com.patta.api.model.ClinicaConfigModel;
import com.patta.api.model.DisponibilidadeDataModel;
import com.patta.api.model.HorarioFuncionamentoModel;
import com.patta.api.model.IntervaloHorarioModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.ClinicaConfigRepository;
import com.patta.api.repository.DisponibilidadeDataRepository;
import com.patta.api.repository.HorarioFuncionamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisponibilidadeServiceTest {

    private static final String ADMIN_UID = "admin";
    private static final String PROFESSIONAL_UID = "professional";
    private final HorarioFuncionamentoRepository horarioRepository = mock(HorarioFuncionamentoRepository.class);
    private final DisponibilidadeDataRepository disponibilidadeDataRepository =
            mock(DisponibilidadeDataRepository.class);
    private final AgendamentoRepository agendamentoRepository = mock(AgendamentoRepository.class);
    private final ClinicaConfigRepository clinicaConfigRepository = mock(ClinicaConfigRepository.class);
    private final ClinicaService clinicaService = new ClinicaService(
            clinicaConfigRepository, new PapelUsuarioService(ADMIN_UID));
    private final DisponibilidadeService service = new DisponibilidadeService(
            horarioRepository, disponibilidadeDataRepository, agendamentoRepository,
            new PapelUsuarioService(ADMIN_UID, PROFESSIONAL_UID), clinicaService);

    @Test
    void listaSlotsComVagasEConsideraReservasAtivas() throws Exception {
        LocalDate data = proxima(DayOfWeek.MONDAY);
        when(horarioRepository.findByServicoAndDia("consulta", "MONDAY"))
                .thenReturn(Optional.of(config("consulta", "MONDAY", 30, 1)));
        when(agendamentoRepository.findByServico("consulta")).thenReturn(List.of(
                AgendamentoModel.builder()
                        .servico("consulta")
                        .data(data.toString())
                        .horario("09:00")
                        .duracaoMinutos(30)
                        .status("PENDENTE")
                        .criadoEm(new Date())
                        .build()));

        var response = service.consultar("consulta", data);

        assertEquals(2, response.horarios().size());
        assertEquals(0, response.horarios().get(0).vagasDisponiveis());
        assertEquals(1, response.horarios().get(1).vagasDisponiveis());
    }

    @Test
    void exigeHorarioConfiguradoParaOservicoEDia() throws Exception {
        LocalDate data = proxima(DayOfWeek.TUESDAY);
        when(horarioRepository.findByServicoAndDia("vacina", "TUESDAY")).thenReturn(Optional.empty());
        when(horarioRepository.findPadrao()).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.consultar("vacina", data));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void naoAceitaHorarioQueNaoEstejaAlinhadoAoSlot() throws Exception {
        LocalDate data = proxima(DayOfWeek.WEDNESDAY);
        when(horarioRepository.findByServicoAndDia("consulta", "WEDNESDAY"))
                .thenReturn(Optional.of(config("consulta", "WEDNESDAY", 30, 1)));
        when(disponibilidadeDataRepository.findByServicoAndData("consulta", data))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarSlot("consulta", data, LocalTime.of(9, 15)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void usaConfiguracaoPorDataAntesDaRegraSemanal() throws Exception {
        LocalDate data = proxima(DayOfWeek.THURSDAY);
        when(disponibilidadeDataRepository.findByServicoAndData("consulta", data))
                .thenReturn(Optional.of(DisponibilidadeDataModel.builder()
                        .id("consulta_" + data)
                        .servico("consulta")
                        .data(data.toString())
                        .intervalos(List.of(IntervaloHorarioModel.builder()
                                .inicio("14:00")
                                .fim("15:00")
                                .build()))
                        .duracaoMinutos(30)
                        .capacidade(1)
                        .build()));
        when(agendamentoRepository.findByServico("consulta")).thenReturn(List.of());

        var response = service.consultar("consulta", data);

        assertEquals(2, response.horarios().size());
        assertEquals(LocalTime.of(14, 0), response.horarios().get(0).horario());
        verify(horarioRepository, org.mockito.Mockito.never())
                .findByServicoAndDia("consulta", "THURSDAY");
    }

    @Test
    void usaHorarioPadraoQuandoNaoHaRegraPorDataNemSemanal() throws Exception {
        LocalDate data = proxima(DayOfWeek.FRIDAY);
        when(disponibilidadeDataRepository.findByServicoAndData("consulta", data))
                .thenReturn(Optional.empty());
        when(horarioRepository.findByServicoAndDia("consulta", "FRIDAY")).thenReturn(Optional.empty());
        when(horarioRepository.findPadrao()).thenReturn(Optional.of(configuracaoPadrao()));
        when(agendamentoRepository.findByServico("consulta")).thenReturn(List.of());

        var response = service.consultar("consulta", data);

        assertEquals(4, response.horarios().size());
        assertEquals(30, response.duracaoMinutos());
        assertEquals(2, response.horarios().get(0).capacidade());
    }

    @Test
    void continuaHorarioPadraoECombinaIntervalosAdjacentes() throws Exception {
        LocalDate data = proxima(DayOfWeek.SATURDAY);
        HorarioFuncionamentoModel semanal = HorarioFuncionamentoModel.builder()
                .servico("consulta")
                .diaSemana("SATURDAY")
                .duracaoMinutos(30)
                .capacidade(1)
                .intervalos(List.of(
                        IntervaloHorarioModel.builder().inicio("08:00").fim("10:00").build(),
                        IntervaloHorarioModel.builder().inicio("14:00").fim("15:00").build()))
                .build();
        semanal.setContinuarHorarioPadrao(true);
        when(disponibilidadeDataRepository.findByServicoAndData("consulta", data))
                .thenReturn(Optional.empty());
        when(horarioRepository.findByServicoAndDia("consulta", "SATURDAY"))
                .thenReturn(Optional.of(semanal));
        when(horarioRepository.findPadrao()).thenReturn(Optional.of(configuracaoPadrao()));
        when(agendamentoRepository.findByServico("consulta")).thenReturn(List.of());

        var response = service.consultar("consulta", data);

        assertEquals(10, response.horarios().size());
        assertEquals(LocalTime.of(8, 0), response.horarios().get(0).horario());
        assertEquals(LocalTime.of(14, 30), response.horarios().getLast().horario());
    }

    @Test
    void exigeExatamenteDoisIntervalosSeparadosNoHorarioPadrao() {
        var request = new HorarioFuncionamentoRequestDTO(List.of(
                new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(12, 0), LocalTime.of(17, 0))),
                30, 2);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.configurarPadrao(PROFESSIONAL_UID, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void profissionalPodeConfigurarConsultarERemoverHorarioPadrao() throws Exception {
        var request = new HorarioFuncionamentoRequestDTO(List.of(
                new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(13, 0), LocalTime.of(17, 0))),
                30, 2);
        when(horarioRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(horarioRepository.findPadrao()).thenReturn(Optional.of(configuracaoPadrao()));

        HorarioFuncionamentoModel saved = service.configurarPadrao(PROFESSIONAL_UID, request);
        HorarioFuncionamentoModel found = service.consultarPadrao(PROFESSIONAL_UID);
        service.removerPadrao(PROFESSIONAL_UID);

        assertEquals("padrao_TODOS", saved.getId());
        assertEquals(30, found.getDuracaoMinutos());
        assertEquals(2, found.getCapacidade());
        verify(horarioRepository).deletePadrao();
    }

    @Test
    void periodoAplicaContinuacaoSomenteNaDataInicial() throws Exception {
        var request = new HorarioFuncionamentoRequestDTO(
                List.of(new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(8, 0), LocalTime.of(12, 0))),
                30, 1, true);
        LocalDate inicio = LocalDate.now().plusDays(1);

        service.configurarPeriodo(ADMIN_UID, "consulta", inicio, inicio.plusDays(2), request);

        verify(disponibilidadeDataRepository).saveAll(org.mockito.ArgumentMatchers.argThat(
                dates -> Boolean.TRUE.equals(dates.get(0).getContinuarHorarioPadrao())
                        && Boolean.FALSE.equals(dates.get(1).getContinuarHorarioPadrao())
                        && Boolean.FALSE.equals(dates.get(2).getContinuarHorarioPadrao())));
    }

    @Test
    void rejeitaConfiguracaoPorHorarioParaHospedagem() {
        var request = new HorarioFuncionamentoRequestDTO(
                List.of(new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(8, 0), LocalTime.of(12, 0))),
                30, 1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.configurar(ADMIN_UID, "hospedagem", "MONDAY", request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void gravaDisponibilidadeEmCadaDataDoPeriodo() throws Exception {
        var request = new HorarioFuncionamentoRequestDTO(
                List.of(new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(8, 0), LocalTime.of(12, 0))),
                30,
                1);
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fim = inicio.plusDays(6);

        service.configurarPeriodo(ADMIN_UID, "consulta", inicio, fim, request);

        verify(disponibilidadeDataRepository).saveAll(org.mockito.ArgumentMatchers.argThat(
                dates -> dates.size() == 7
                        && dates.get(0).getData().equals(inicio.toString())
                        && dates.get(6).getData().equals(fim.toString())));
    }

    @Test
    void rejeitaPeriodoMaiorQueUmMes() {
        var request = new HorarioFuncionamentoRequestDTO(
                List.of(new HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO(
                        LocalTime.of(8, 0), LocalTime.of(12, 0))),
                30,
                1);
        LocalDate inicio = LocalDate.now();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.configurarPeriodo(ADMIN_UID, "consulta", inicio, inicio.plusDays(31), request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void consultaHospedagemUsaCapacidadeGlobalNoPeriodoSemLerHorarios() throws Exception {
        LocalDate data = LocalDate.now().plusDays(1);
        when(clinicaConfigRepository.buscar()).thenReturn(ClinicaConfigModel.builder()
                .capacidadeHospedagem(4)
                .build());
        when(agendamentoRepository.picoOcupacaoHospedagem(
                LocalDateTime.of(data, LocalTime.MIDNIGHT),
                LocalDateTime.of(data.plusDays(5), LocalTime.MIDNIGHT))).thenReturn(3);

        var response = service.consultar("hospedagem", data, 5);

        assertNull(response.duracaoMinutos());
        assertEquals(4, response.capacidadeHospedagem());
        assertEquals(1, response.vagasDisponiveis());
        assertEquals(List.of(), response.horarios());
        verify(disponibilidadeDataRepository, org.mockito.Mockito.never())
                .findByServicoAndData("hospedagem", data);
        verify(horarioRepository, org.mockito.Mockito.never())
                .findByServicoAndDia("hospedagem", data.getDayOfWeek().name());
    }

    @Test
    void consultaHospedagemSemCapacidadeNaoRetornaVagas() throws Exception {
        LocalDate data = LocalDate.now().plusDays(1);
        when(clinicaConfigRepository.buscar()).thenReturn(ClinicaConfigModel.builder().build());
        when(agendamentoRepository.picoOcupacaoHospedagem(
                LocalDateTime.of(data, LocalTime.MIDNIGHT),
                LocalDateTime.of(data.plusDays(2), LocalTime.MIDNIGHT))).thenReturn(0);

        var response = service.consultar("hospedagem", data, 2);

        assertNull(response.capacidadeHospedagem());
        assertEquals(0, response.vagasDisponiveis());
    }

    @Test
    void rejeitaValidacaoDeSlotParaHospedagemSemConsultarHorarios() throws Exception {
        LocalDate data = LocalDate.now().plusDays(1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarSlot("hospedagem", data, LocalTime.NOON));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(disponibilidadeDataRepository, org.mockito.Mockito.never())
                .findByServicoAndData("hospedagem", data);
        verify(horarioRepository, org.mockito.Mockito.never())
                .findByServicoAndDia("hospedagem", data.getDayOfWeek().name());
    }

    private HorarioFuncionamentoModel config(String servico, String dia, int duracao, int capacidade) {
        return HorarioFuncionamentoModel.builder()
                .servico(servico)
                .diaSemana(dia)
                .duracaoMinutos(duracao)
                .capacidade(capacidade)
                .intervalos(List.of(IntervaloHorarioModel.builder()
                        .inicio("09:00")
                        .fim("10:00")
                        .build()))
                .build();
    }

    private HorarioFuncionamentoModel configuracaoPadrao() {
        return HorarioFuncionamentoModel.builder()
                .id("padrao_TODOS")
                .servico("padrao")
                .diaSemana("TODOS")
                .duracaoMinutos(30)
                .capacidade(2)
                .intervalos(List.of(
                        IntervaloHorarioModel.builder().inicio("10:00").fim("11:00").build(),
                        IntervaloHorarioModel.builder().inicio("13:00").fim("14:00").build()))
                .build();
    }

    private LocalDate proxima(DayOfWeek dia) {
        LocalDate data = LocalDate.now();
        do {
            data = data.plusDays(1);
        } while (data.getDayOfWeek() != dia);
        return data;
    }
}
