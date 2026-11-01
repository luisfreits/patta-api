package com.patta.api.service;

import com.patta.api.model.AgendamentoModel;
import com.patta.api.model.HorarioFuncionamentoModel;
import com.patta.api.model.IntervaloHorarioModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.HorarioFuncionamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisponibilidadeServiceTest {

    private static final String ADMIN_UID = "admin";
    private final HorarioFuncionamentoRepository horarioRepository = mock(HorarioFuncionamentoRepository.class);
    private final AgendamentoRepository agendamentoRepository = mock(AgendamentoRepository.class);
    private final DisponibilidadeService service = new DisponibilidadeService(
            horarioRepository, agendamentoRepository, new PapelUsuarioService(ADMIN_UID));

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

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.consultar("vacina", data));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void naoAceitaHorarioQueNaoEstejaAlinhadoAoSlot() throws Exception {
        LocalDate data = proxima(DayOfWeek.WEDNESDAY);
        when(horarioRepository.findByServicoAndDia("consulta", "WEDNESDAY"))
                .thenReturn(Optional.of(config("consulta", "WEDNESDAY", 30, 1)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarSlot("consulta", data, LocalTime.of(9, 15)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
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

    private LocalDate proxima(DayOfWeek dia) {
        LocalDate data = LocalDate.now();
        do {
            data = data.plusDays(1);
        } while (data.getDayOfWeek() != dia);
        return data;
    }
}
