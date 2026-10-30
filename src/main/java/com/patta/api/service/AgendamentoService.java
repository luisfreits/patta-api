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

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              AnimalService animalService,
                              PapelUsuarioService papelUsuarioService) {
        this.agendamentoRepository = agendamentoRepository;
        this.animalService = animalService;
        this.papelUsuarioService = papelUsuarioService;
    }

    public AgendamentoResponseDTO criar(String uid, AgendamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        animalService.buscar(uid, request.animalId());
        String servico = request.servico().trim().toLowerCase(Locale.ROOT);
        if (!SERVICOS_PERMITIDOS.contains(servico)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O serviço solicitado não é válido.");
        }
        if (request.data().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data não pode estar no passado.");
        }
        validarHorario(request.horario());

        Instant agora = Instant.now();
        AgendamentoModel agendamento = AgendamentoModel.builder()
                .usuarioId(uid)
                .animalId(request.animalId().trim())
                .servico(servico)
                .data(request.data().toString())
                .horario(request.horario().toString())
                .status(AgendamentoStatus.PENDENTE.name())
                .observacoes(normalizarObservacoes(request.observacoes()))
                .criadoEm(Date.from(agora))
                .atualizadoEm(Date.from(agora))
                .build();
        return toResponse(agendamentoRepository.save(agendamento));
    }

    public List<AgendamentoResponseDTO> listar(String uid)
            throws ExecutionException, InterruptedException {
        List<AgendamentoModel> agendamentos = isAdministrador(uid)
                ? agendamentoRepository.findAll()
                : agendamentoRepository.findByUsuarioId(uid);
        return agendamentos.stream().map(this::toResponse).toList();
    }

    public AgendamentoResponseDTO buscar(String uid, String id)
            throws ExecutionException, InterruptedException {
        AgendamentoModel agendamento = buscarExistente(id);
        if (!uid.equals(agendamento.getUsuarioId()) && !isAdministrador(uid)) {
            throw naoEncontrado();
        }
        return toResponse(agendamento);
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

    private AgendamentoModel buscarExistente(String id) throws ExecutionException, InterruptedException {
        return agendamentoRepository.findById(id).orElseThrow(AgendamentoService::naoEncontrado);
    }

    private void exigirAdministrador(String uid) {
        if (!isAdministrador(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores autorizados podem alterar o status.");
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
                agendamento.getStatus(),
                agendamento.getObservacoes(),
                agendamento.getCriadoEm().toInstant(),
                agendamento.getAtualizadoEm().toInstant());
    }
}
