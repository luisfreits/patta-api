package com.patta.api.service;

import com.patta.api.dto.AgendamentoRequestDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.AgendamentoStatusRequestDTO;
import com.patta.api.model.AgendamentoModel;
import com.patta.api.model.AgendamentoStatus;
import com.patta.api.repository.AgendamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
public class AgendamentoService {

    private static final Set<String> SERVICOS_PERMITIDOS = Set.of("consulta", "vacina", "hospedagem");

    private final AgendamentoRepository agendamentoRepository;
    private final AnimalService animalService;
    private final PapelUsuarioService papelUsuarioService;
    private final DisponibilidadeService disponibilidadeService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              AnimalService animalService,
                              PapelUsuarioService papelUsuarioService,
                              DisponibilidadeService disponibilidadeService) {
        this.agendamentoRepository = agendamentoRepository;
        this.animalService = animalService;
        this.papelUsuarioService = papelUsuarioService;
        this.disponibilidadeService = disponibilidadeService;
    }

    public AgendamentoResponseDTO criar(String uid, AgendamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        animalService.buscar(uid, request.animalId());
        return criarParaDono(uid, request);
    }

    public AgendamentoResponseDTO criarParaUsuario(String administradorUid, String usuarioUid,
                                                    AgendamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(administradorUid);
        animalService.buscar(usuarioUid, request.animalId());
        return criarParaDono(usuarioUid, request);
    }

    private AgendamentoResponseDTO criarParaDono(String uid, AgendamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        String servico = request.servico().trim().toLowerCase(Locale.ROOT);
        if (!SERVICOS_PERMITIDOS.contains(servico)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O serviço solicitado não é válido.");
        }
        if (request.data().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data não pode estar no passado.");
        }
        validarHorario(request.horario());
        if (request.data().isEqual(LocalDate.now()) && !request.horario().isAfter(LocalTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O horário do agendamento não pode estar no passado.");
        }
        if ("hospedagem".equals(servico)) {
            if (request.dataSaidaPrevista() == null
                    || !request.dataSaidaPrevista().isAfter(request.data())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A data de saída prevista deve ser posterior à data de entrada.");
            }
        } else if (request.dataSaidaPrevista() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data de saída prevista só se aplica a hospedagens.");
        }

        var disponibilidade = disponibilidadeService.validarSlot(servico, request.data(), request.horario());

        Instant agora = Instant.now();
        AgendamentoModel agendamento = AgendamentoModel.builder()
                .usuarioId(uid)
                .animalId(request.animalId().trim())
                .servico(servico)
                .data(request.data().toString())
                .horario(request.horario().toString())
                .dataSaidaPrevista(request.dataSaidaPrevista() == null
                        ? null
                        : request.dataSaidaPrevista().toString())
                .duracaoMinutos(disponibilidade.getDuracaoMinutos())
                .status(AgendamentoStatus.PENDENTE.name())
                .observacoes(normalizarObservacoes(request.observacoes()))
                .criadoEm(Date.from(agora))
                .atualizadoEm(Date.from(agora))
                .build();
        try {
            return toResponse(agendamentoRepository.saveIfAvailable(agendamento, disponibilidade.getCapacidade()));
        } catch (AgendamentoRepository.ConflitoReservaException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O horário ou a capacidade de hospedagem não está mais disponível.", exception);
        }
    }

    public List<AgendamentoResponseDTO> listar(String uid)
            throws ExecutionException, InterruptedException {
        return isAdministrador(uid)
                ? listarTodos(uid)
                : agendamentoRepository.findByUsuarioId(uid).stream().map(this::toResponse).toList();
    }

    public List<AgendamentoResponseDTO> listarTodos(String uid)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        return agendamentoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<AgendamentoResponseDTO> listarParaEquipe(String uid)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        return agendamentoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AgendamentoResponseDTO buscar(String uid, String id)
            throws ExecutionException, InterruptedException {
        AgendamentoModel agendamento = buscarExistente(id);
        if (!uid.equals(agendamento.getUsuarioId()) && !papelUsuarioService.ehAdministrador(uid)) {
            throw naoEncontrado();
        }
        return toResponse(agendamento);
    }

    public AgendamentoResponseDTO buscarParaEquipe(String uid, String id)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        return toResponse(buscarExistente(id));
    }

    public AgendamentoResponseDTO alterarStatus(String uid, String id, AgendamentoStatusRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        AgendamentoModel agendamento = buscarExistente(id);
        if (!AgendamentoStatus.PENDENTE.name().equals(agendamento.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente agendamentos pendentes podem ser aprovados ou rejeitados.");
        }
        if (request.status() != AgendamentoStatus.APROVADO
                && request.status() != AgendamentoStatus.REJEITADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O administrador só pode aprovar ou rejeitar um agendamento pendente.");
        }
        agendamento.setStatus(request.status().name());
        agendamento.setAtualizadoEm(Date.from(Instant.now()));
        return toResponse(agendamentoRepository.save(agendamento));
    }

    public AgendamentoResponseDTO alterarDescricao(String uid, String id, String descricao)
            throws ExecutionException, InterruptedException {
        AgendamentoModel agendamento = buscarExistente(id);
            if (!uid.equals(agendamento.getUsuarioId()) && !papelUsuarioService.ehAdministrador(uid)) throw naoEncontrado();
        if (agendamento.getCheckInEm() != null
                || AgendamentoStatus.REJEITADO.name().equals(agendamento.getStatus())
                || AgendamentoStatus.CANCELADO.name().equals(agendamento.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A descrição não pode ser alterada após o check-in ou encerramento do agendamento.");
        }
        agendamento.setObservacoes(normalizarObservacoes(descricao));
        agendamento.setAtualizadoEm(Date.from(Instant.now()));
        return toResponse(agendamentoRepository.save(agendamento));
    }

    public AgendamentoResponseDTO marcarCheckIn(String uid, String id)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        AgendamentoModel agendamento = buscarExistente(id);
        if (!AgendamentoStatus.APROVADO.name().equals(agendamento.getStatus())
                || agendamento.getCheckInEm() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O check-in exige um agendamento aprovado ainda não recebido.");
        }
        agendamento.setCheckInEm(Date.from(Instant.now()));
        agendamento.setCheckInPor(uid);
        agendamento.setAtualizadoEm(Date.from(Instant.now()));
        return toResponse(agendamentoRepository.save(agendamento));
    }

    public AgendamentoResponseDTO marcarCheckOut(String uid, String id)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        AgendamentoModel agendamento = buscarExistente(id);
        if (!"hospedagem".equals(agendamento.getServico())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O check-out só se aplica a agendamentos de hospedagem.");
        }
        if (agendamento.getCheckInEm() == null || agendamento.getCheckOutEm() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O check-out exige uma hospedagem com check-in realizado e ainda não encerrada.");
        }
        agendamento.setCheckOutEm(Date.from(Instant.now()));
        agendamento.setCheckOutPor(uid);
        agendamento.setAtualizadoEm(Date.from(Instant.now()));
        return toResponse(agendamentoRepository.save(agendamento));
    }

    private AgendamentoModel buscarExistente(String id) throws ExecutionException, InterruptedException {
        return agendamentoRepository.findById(id).orElseThrow(AgendamentoService::naoEncontrado);
    }

    private void exigirAdministrador(String uid) {
        if (!isAdministrador(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores autorizados podem alterar o status.");
        }
    }

    private void exigirEquipe(String uid) {
        if (!papelUsuarioService.podeGerenciarAtendimento(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores ou profissionais autorizados podem registrar atendimento.");
        }
    }

    private boolean isAdministrador(String uid) {
        return papelUsuarioService.ehAdministrador(uid);
    }

    private static ResponseStatusException naoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado.");
    }

    private String normalizarObservacoes(String observacoes) {
        return observacoes == null || observacoes.isBlank() ? null : observacoes.trim();
    }

    private void validarHorario(LocalTime horario) {
        if (horario == null || horario.getSecond() != 0 || horario.getNano() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O horário deve ser informado no formato HH:mm.");
        }
    }

    private AgendamentoResponseDTO toResponse(AgendamentoModel agendamento) {
        return new AgendamentoResponseDTO(
                agendamento.getId(),
                agendamento.getUsuarioId(),
                agendamento.getAnimalId(),
                agendamento.getServico(),
                LocalDate.parse(agendamento.getData()),
                LocalTime.parse(agendamento.getHorario()),
                agendamento.getDataSaidaPrevista() == null
                        ? null
                        : LocalDate.parse(agendamento.getDataSaidaPrevista()),
                agendamento.getDuracaoMinutos(),
                agendamento.getStatus(),
                agendamento.getObservacoes(),
                agendamento.getCheckInEm() == null ? null : agendamento.getCheckInEm().toInstant(),
                agendamento.getCheckInPor(),
                agendamento.getCheckOutEm() == null ? null : agendamento.getCheckOutEm().toInstant(),
                agendamento.getCheckOutPor(),
                agendamento.getCriadoEm().toInstant(),
                agendamento.getAtualizadoEm().toInstant());
    }
}
