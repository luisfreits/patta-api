package com.patta.api.controller;

import com.patta.api.dto.RegistroClinicoRequestDTO;
import com.patta.api.dto.VacinaRequestDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.ProfissionalAgendamentoResponseDTO;
import com.patta.api.service.AgendamentoService;
import com.patta.api.service.ProfissionalAgendamentoService;
import com.patta.api.service.RegistroAnimalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/profissional/agendamentos")
@RequiredArgsConstructor
public class ProfissionalController {

    private final RegistroAnimalService registroAnimalService;
    private final AgendamentoService agendamentoService;
    private final ProfissionalAgendamentoService profissionalAgendamentoService;

    @GetMapping
    public List<ProfissionalAgendamentoResponseDTO> listarAgendamentos(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return profissionalAgendamentoService.listar(uid);
    }

    @PostMapping("/{id}/historico-clinico")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrarHistoricoClinico(
            @AuthenticationPrincipal String uid,
            @PathVariable String id,
            @Valid @RequestBody RegistroClinicoRequestDTO request)
            throws ExecutionException, InterruptedException {
        AgendamentoResponseDTO agendamento = buscarAgendamentoAtendido(uid, id);
        profissionalAgendamentoService.buscar(uid, agendamento);
        registroAnimalService.registrarHistoricoClinico(uid, agendamento.animalId(), request);
    }

    @PostMapping("/{id}/vacinas")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrarVacina(
            @AuthenticationPrincipal String uid,
            @PathVariable String id,
            @Valid @RequestBody VacinaRequestDTO request)
            throws ExecutionException, InterruptedException {
        AgendamentoResponseDTO agendamento = buscarAgendamentoAtendido(uid, id);
        profissionalAgendamentoService.buscar(uid, agendamento);
        registroAnimalService.registrarVacina(uid, agendamento.animalId(), request);
    }

    @PostMapping("/{id}/check-in")
    public ProfissionalAgendamentoResponseDTO marcarCheckIn(
            @AuthenticationPrincipal String uid, @PathVariable String id)
            throws ExecutionException, InterruptedException {
        profissionalAgendamentoService.buscar(uid, agendamentoService.buscarParaEquipe(uid, id));
        return profissionalAgendamentoService.buscar(uid, agendamentoService.marcarCheckIn(uid, id));
    }

    @PostMapping("/{id}/check-out")
    public ProfissionalAgendamentoResponseDTO marcarCheckOut(
            @AuthenticationPrincipal String uid, @PathVariable String id)
            throws ExecutionException, InterruptedException {
        profissionalAgendamentoService.buscar(uid, agendamentoService.buscarParaEquipe(uid, id));
        return profissionalAgendamentoService.buscar(uid, agendamentoService.marcarCheckOut(uid, id));
    }

    private AgendamentoResponseDTO buscarAgendamentoAtendido(String uid, String id)
            throws ExecutionException, InterruptedException {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarParaEquipe(uid, id);
        if (agendamento.checkInEm() == null || agendamento.checkOutEm() != null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "O registro clínico exige atendimento em andamento.");
        }
        return agendamento;
    }
}
