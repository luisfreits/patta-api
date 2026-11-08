package com.patta.api.service;

import com.patta.api.dto.DisponibilidadeResponseDTO;
import com.patta.api.dto.HorarioFuncionamentoRequestDTO;
import com.patta.api.dto.HorarioSlotDTO;
import com.patta.api.model.AgendamentoModel;
import com.patta.api.model.DisponibilidadeDataModel;
import com.patta.api.model.HorarioFuncionamentoModel;
import com.patta.api.model.IntervaloHorarioModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.DisponibilidadeDataRepository;
import com.patta.api.repository.HorarioFuncionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private static final List<String> SERVICOS = List.of("consulta", "vacina", "hospedagem");

    private final HorarioFuncionamentoRepository horarioRepository;
    private final DisponibilidadeDataRepository disponibilidadeDataRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PapelUsuarioService papelUsuarioService;
    private final ClinicaService clinicaService;

    public HorarioFuncionamentoModel configurar(String uid, String servico, String diaSemana,
                                                HorarioFuncionamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        String servicoNormalizado = normalizarServico(servico);
        exigirServicoPorHorario(servicoNormalizado);
        String diaNormalizado = normalizarDia(diaSemana);
        validarConfiguracao(request);
        HorarioFuncionamentoModel horario = HorarioFuncionamentoModel.builder()
                .id(HorarioFuncionamentoRepository.id(servicoNormalizado, diaNormalizado))
                .servico(servicoNormalizado)
                .diaSemana(diaNormalizado)
                .duracaoMinutos(request.duracaoMinutos())
                .capacidade(request.capacidade())
                .intervalos(intervalosModel(request))
                .continuarHorarioPadrao(request.continuarHorarioPadrao())
                .build();
        return horarioRepository.save(horario);
    }

    public void configurarPeriodo(String uid, String servico, LocalDate inicio, LocalDate fim,
                                  HorarioFuncionamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        String servicoNormalizado = normalizarServico(servico);
        exigirServicoPorHorario(servicoNormalizado);
        validarPeriodo(inicio, fim);
        validarConfiguracao(request);
        List<DisponibilidadeDataModel> disponibilidades = new ArrayList<>();
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            disponibilidades.add(DisponibilidadeDataModel.builder()
                    .id(DisponibilidadeDataRepository.id(servicoNormalizado, data))
                    .servico(servicoNormalizado)
                    .data(data.toString())
                    .duracaoMinutos(request.duracaoMinutos())
                    .capacidade(request.capacidade())
                    .intervalos(intervalosModel(request))
                    .continuarHorarioPadrao(request.continuarHorarioPadrao() && data.equals(inicio))
                    .build());
        }
        disponibilidadeDataRepository.saveAll(disponibilidades);
    }

    public HorarioFuncionamentoModel consultarPadrao(String uid)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        return horarioRepository.findPadrao()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Não há horário padrão configurado."));
    }

    public HorarioFuncionamentoModel configurarPadrao(String uid, HorarioFuncionamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        validarConfiguracao(request);
        validarIntervalosPadrao(request.intervalos());
        HorarioFuncionamentoModel padrao = HorarioFuncionamentoModel.builder()
                .id(HorarioFuncionamentoRepository.id("padrao", "TODOS"))
                .servico("padrao")
                .diaSemana("TODOS")
                .intervalos(intervalosModel(request))
                .duracaoMinutos(request.duracaoMinutos())
                .capacidade(request.capacidade())
                .continuarHorarioPadrao(false)
                .build();
        return horarioRepository.save(padrao);
    }

    public void removerPadrao(String uid) throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        horarioRepository.deletePadrao();
    }

    public void remover(String uid, String servico, String diaSemana)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        horarioRepository.delete(normalizarServico(servico), normalizarDia(diaSemana));
    }

    public void removerPeriodo(String uid, String servico, LocalDate inicio, LocalDate fim)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        validarPeriodo(inicio, fim);
        disponibilidadeDataRepository.deleteRange(normalizarServico(servico), inicio, fim);
    }

    public DisponibilidadeResponseDTO consultar(String servico, LocalDate data)
            throws ExecutionException, InterruptedException {
        return consultar(servico, data, null);
    }

    public DisponibilidadeResponseDTO consultar(String servico, LocalDate data, Integer tempoHospedagemDias)
            throws ExecutionException, InterruptedException {
        String servicoNormalizado = normalizarServico(servico);
        if (data.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data não pode estar no passado.");
        }
        if ("hospedagem".equals(servicoNormalizado)) {
            if (tempoHospedagemDias == null || tempoHospedagemDias < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O tempo de hospedagem deve ser de pelo menos 1 dia.");
            }
            LocalDateTime inicio = data.atStartOfDay();
            LocalDateTime fim = inicio.plusDays(tempoHospedagemDias);
            Integer capacidade = clinicaService.consultarCapacidadeHospedagem();
            int ocupacaoMaxima = agendamentoRepository.picoOcupacaoHospedagem(inicio, fim);
            int vagasDisponiveis = capacidade == null
                    ? 0
                    : Math.max(0, capacidade - ocupacaoMaxima);
            return new DisponibilidadeResponseDTO(servicoNormalizado, data, null, List.of(),
                    capacidade, vagasDisponiveis);
        }
        if (tempoHospedagemDias != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tempo de hospedagem só se aplica a hospedagens.");
        }
        HorarioFuncionamentoModel config = buscarConfiguracao(servicoNormalizado, data);
        List<AgendamentoModel> reservas = agendamentoRepository.findByServico(servicoNormalizado);
        List<HorarioSlotDTO> slots = new ArrayList<>();
        for (IntervaloHorarioModel intervalo : config.getIntervalos()) {
            LocalTime inicio = LocalTime.parse(intervalo.getInicio());
            LocalTime fim = LocalTime.parse(intervalo.getFim());
            while (!inicio.plusMinutes(config.getDuracaoMinutos()).isAfter(fim)) {
                if (data.isAfter(LocalDate.now()) || inicio.isAfter(LocalTime.now())) {
                    int ocupadas = contarReservasNoSlot(reservas, servicoNormalizado, data, inicio,
                            config.getDuracaoMinutos());
                    slots.add(new HorarioSlotDTO(inicio, config.getCapacidade(),
                            Math.max(0, config.getCapacidade() - ocupadas)));
                }
                inicio = inicio.plusMinutes(config.getDuracaoMinutos());
            }
        }
        slots.sort(Comparator.comparing(HorarioSlotDTO::horario));
        return new DisponibilidadeResponseDTO(servicoNormalizado, data, config.getDuracaoMinutos(), slots);
    }

    public HorarioFuncionamentoModel validarSlot(String servico, LocalDate data, LocalTime horario)
            throws ExecutionException, InterruptedException {
        String servicoNormalizado = normalizarServico(servico);
        if ("hospedagem".equals(servicoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A hospedagem não usa validação de horário de atendimento.");
        }
        HorarioFuncionamentoModel config = buscarConfiguracao(servicoNormalizado, data);
        for (IntervaloHorarioModel intervalo : config.getIntervalos()) {
            LocalTime inicio = LocalTime.parse(intervalo.getInicio());
            LocalTime fim = LocalTime.parse(intervalo.getFim());
            if (horario.isBefore(inicio) || horario.plusMinutes(config.getDuracaoMinutos()).isAfter(fim)) {
                continue;
            }
            long minutos = java.time.Duration.between(inicio, horario).toMinutes();
            if (minutos % config.getDuracaoMinutos() == 0) return config;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O horário solicitado não está disponível na grade configurada.");
    }

    private HorarioFuncionamentoModel buscarConfiguracao(String servico, LocalDate data)
            throws ExecutionException, InterruptedException {
        var configuracaoDaData = disponibilidadeDataRepository.findByServicoAndData(servico, data);
        if (configuracaoDaData.isPresent()) {
            var override = configuracaoDaData.get();
            return aplicarPadraoSeSolicitado(HorarioFuncionamentoModel.builder()
                    .id(override.getId())
                    .servico(override.getServico())
                    .diaSemana(data.getDayOfWeek().name())
                    .intervalos(override.getIntervalos())
                    .duracaoMinutos(override.getDuracaoMinutos())
                    .capacidade(override.getCapacidade())
                    .continuarHorarioPadrao(override.getContinuarHorarioPadrao())
                    .build());
        }
        var configuracaoSemanal = horarioRepository.findByServicoAndDia(servico, data.getDayOfWeek().name());
        if (configuracaoSemanal.isPresent()) {
            return aplicarPadraoSeSolicitado(configuracaoSemanal.get());
        }
        HorarioFuncionamentoModel padrao = horarioRepository.findPadrao()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Não há horário de funcionamento configurado para essa data."));
        return HorarioFuncionamentoModel.builder()
                .id(padrao.getId())
                .servico(servico)
                .diaSemana(data.getDayOfWeek().name())
                .intervalos(padrao.getIntervalos())
                .duracaoMinutos(padrao.getDuracaoMinutos())
                .capacidade(padrao.getCapacidade())
                .continuarHorarioPadrao(false)
                .build();
    }

    private HorarioFuncionamentoModel aplicarPadraoSeSolicitado(HorarioFuncionamentoModel configuracao)
            throws ExecutionException, InterruptedException {
        if (!Boolean.TRUE.equals(configuracao.getContinuarHorarioPadrao())) {
            return configuracao;
        }
        var padrao = horarioRepository.findPadrao();
        if (padrao.isEmpty()) {
            return configuracao;
        }
        configuracao.setIntervalos(combinarIntervalos(
                configuracao.getIntervalos(), padrao.get().getIntervalos()));
        return configuracao;
    }

    private List<IntervaloHorarioModel> combinarIntervalos(
            List<IntervaloHorarioModel> intervalosRegra, List<IntervaloHorarioModel> intervalosPadrao) {
        List<IntervaloHorarioModel> ordenados = new ArrayList<>();
        if (intervalosRegra != null) ordenados.addAll(intervalosRegra);
        if (intervalosPadrao != null) ordenados.addAll(intervalosPadrao);
        ordenados.sort(Comparator.comparing(intervalo -> LocalTime.parse(intervalo.getInicio())));

        List<IntervaloHorarioModel> combinados = new ArrayList<>();
        for (IntervaloHorarioModel intervalo : ordenados) {
            if (combinados.isEmpty()) {
                combinados.add(copiarIntervalo(intervalo));
                continue;
            }
            IntervaloHorarioModel anterior = combinados.getLast();
            LocalTime fimAnterior = LocalTime.parse(anterior.getFim());
            LocalTime inicioAtual = LocalTime.parse(intervalo.getInicio());
            if (inicioAtual.isAfter(fimAnterior)) {
                combinados.add(copiarIntervalo(intervalo));
            } else if (LocalTime.parse(intervalo.getFim()).isAfter(fimAnterior)) {
                anterior.setFim(intervalo.getFim());
            }
        }
        return List.copyOf(combinados);
    }

    private IntervaloHorarioModel copiarIntervalo(IntervaloHorarioModel intervalo) {
        return IntervaloHorarioModel.builder()
                .inicio(intervalo.getInicio())
                .fim(intervalo.getFim())
                .build();
    }

    private List<IntervaloHorarioModel> intervalosModel(HorarioFuncionamentoRequestDTO request) {
        return request.intervalos().stream()
                .map(intervalo -> IntervaloHorarioModel.builder()
                        .inicio(intervalo.inicio().toString())
                        .fim(intervalo.fim().toString())
                        .build())
                .sorted(Comparator.comparing(intervalo -> LocalTime.parse(intervalo.getInicio())))
                .toList();
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data final do período deve ser igual ou posterior à data inicial.");
        }
        if (fim.toEpochDay() - inicio.toEpochDay() >= 31) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período não pode ultrapassar 31 dias.");
        }
    }

    private int contarReservasNoSlot(List<AgendamentoModel> reservas, String servico, LocalDate data,
                                     LocalTime inicioSlot, int duracaoMinutos) {
        int ocupadas = 0;
        LocalDateTime inicio = LocalDateTime.of(data, inicioSlot);
        LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);
        for (AgendamentoModel reserva : reservas) {
            if (!reservaAtiva(reserva)) continue;
            LocalDate dataReserva = LocalDate.parse(reserva.getData());
            if ("hospedagem".equals(servico)) {
                LocalDate saida = reserva.getDataSaidaPrevista() == null
                        ? dataReserva.plusDays(1)
                        : LocalDate.parse(reserva.getDataSaidaPrevista());
                if (!data.isBefore(dataReserva) && data.isBefore(saida)) ocupadas++;
                continue;
            }
            if (!data.equals(dataReserva)) continue;
            LocalDateTime inicioReserva = LocalDateTime.of(dataReserva, LocalTime.parse(reserva.getHorario()));
            int duracaoReserva = reserva.getDuracaoMinutos() == null
                    ? duracaoMinutos
                    : reserva.getDuracaoMinutos();
            LocalDateTime fimReserva = inicioReserva.plusMinutes(duracaoReserva);
            if (inicio.isBefore(fimReserva) && inicioReserva.isBefore(fim)) ocupadas++;
        }
        return ocupadas;
    }

    private boolean reservaAtiva(AgendamentoModel agendamento) {
        return !"REJEITADO".equals(agendamento.getStatus())
                && !"CANCELADO".equals(agendamento.getStatus())
                && agendamento.getCheckOutEm() == null;
    }

    private void validarIntervalos(List<HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO> intervalos) {
        if (intervalos == null || intervalos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "É necessário informar ao menos um intervalo de funcionamento.");
        }
        List<HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO> ordenados = intervalos.stream()
                .sorted(Comparator.comparing(HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO::inicio))
                .toList();
        for (int indice = 0; indice < ordenados.size(); indice++) {
            var atual = ordenados.get(indice);
            if (!atual.inicio().isBefore(atual.fim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O início de cada intervalo deve ser anterior ao fim.");
            }
            if (indice > 0 && ordenados.get(indice - 1).fim().isAfter(atual.inicio())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Os intervalos de funcionamento não podem se sobrepor.");
            }
        }
    }

    private void validarIntervalosPadrao(List<HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO> intervalos) {
        if (intervalos == null || intervalos.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O horário padrão deve conter exatamente dois intervalos.");
        }
        validarIntervalos(intervalos);
        List<HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO> ordenados = intervalos.stream()
                .sorted(Comparator.comparing(HorarioFuncionamentoRequestDTO.IntervaloHorarioDTO::inicio))
                .toList();
        if (!ordenados.get(1).inicio().isAfter(ordenados.get(0).fim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O segundo intervalo do padrão deve começar depois do fim do primeiro.");
        }
    }

    private void validarConfiguracao(HorarioFuncionamentoRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A configuração de horário é obrigatória.");
        }
        validarIntervalos(request.intervalos());
        if (request.duracaoMinutos() == null || request.duracaoMinutos() < 5
                || request.duracaoMinutos() > 1440) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A duração do serviço deve estar entre 5 e 1440 minutos.");
        }
        if (request.capacidade() == null || request.capacidade() < 1 || request.capacidade() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A capacidade deve estar entre 1 e 100.");
        }
    }

    private void exigirServicoPorHorario(String servico) {
        if ("hospedagem".equals(servico)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A hospedagem não usa configuração por horário de atendimento.");
        }
    }

    private String normalizarServico(String servico) {
        String normalizado = servico == null ? "" : servico.trim().toLowerCase(Locale.ROOT);
        if (!SERVICOS.contains(normalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O serviço informado não é válido.");
        }
        return normalizado;
    }

    private String normalizarDia(String diaSemana) {
        try {
            return DayOfWeek.valueOf(diaSemana.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O dia da semana deve ser MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY ou SUNDAY.");
        }
    }

    private void exigirAdministrador(String uid) {
        if (!papelUsuarioService.ehAdministrador(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores autorizados podem configurar horários.");
        }
    }

    private void exigirEquipe(String uid) {
        if (!papelUsuarioService.podeGerenciarAtendimento(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente profissionais ou administradores autorizados podem configurar o horário padrão.");
        }
    }
}
