package com.patta.api.service;

import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.ProfissionalAgendamentoResponseDTO;
import com.patta.api.model.AgendamentoStatus;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ProfissionalAgendamentoService {

    private final AgendamentoService agendamentoService;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final PapelUsuarioService papelUsuarioService;

    public List<ProfissionalAgendamentoResponseDTO> listar(String uid)
            throws ExecutionException, InterruptedException {
        List<AgendamentoResponseDTO> agendamentos = agendamentoService.listarParaEquipe(uid);
        List<ProfissionalAgendamentoResponseDTO> respostas = new ArrayList<>();
        for (AgendamentoResponseDTO agendamento : agendamentos) {
            if (!papelUsuarioService.ehAdministrador(uid)
                    && !"USER".equals(papelUsuarioService.buscarPapel(agendamento.usuarioId()))) {
                continue;
            }
            respostas.add(toResponse(agendamento, buscarTutor(agendamento.usuarioId()),
                    buscarPet(agendamento.animalId())));
        }
        return List.copyOf(respostas);
    }

    public ProfissionalAgendamentoResponseDTO buscar(String uid, AgendamentoResponseDTO agendamento)
            throws ExecutionException, InterruptedException {
        if (!papelUsuarioService.ehAdministrador(uid)
                && !"USER".equals(papelUsuarioService.buscarPapel(agendamento.usuarioId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado.");
        }
        return toResponse(agendamento, buscarTutor(agendamento.usuarioId()), buscarPet(agendamento.animalId()));
    }

    private String buscarTutor(String uid) throws ExecutionException, InterruptedException {
        return userRepository.findById(uid)
                .map(usuario -> usuario.getNome() == null ? "" : usuario.getNome())
                .orElse("Tutor");
    }

    private String buscarPet(String id) throws ExecutionException, InterruptedException {
        return animalRepository.findById(id)
                .map(animal -> animal.getNome() == null ? "" : animal.getNome())
                .orElse("Pet");
    }

    private ProfissionalAgendamentoResponseDTO toResponse(
            AgendamentoResponseDTO agendamento, String tutorNome, String petNome) {
        boolean hospedagem = "hospedagem".equals(agendamento.servico());
        boolean checkInDisponivel = AgendamentoStatus.APROVADO.name().equals(agendamento.status())
                && agendamento.checkInEm() == null;
        boolean checkOutDisponivel = hospedagem
                && agendamento.checkInEm() != null
                && agendamento.checkOutEm() == null;
        boolean atendimentoDisponivel = agendamento.checkInEm() != null
                && agendamento.checkOutEm() == null;

        return new ProfissionalAgendamentoResponseDTO(
                agendamento.id(), tutorNome, petNome, agendamento.data(), agendamento.horario(),
                agendamento.dataSaidaPrevista(), agendamento.tempoHospedagemDias(), agendamento.status(),
                agendamento.permanenciaExcedente(), agendamento.permanenciaExcedenteDescricao(),
                hospedagem, checkInDisponivel, checkOutDisponivel, atendimentoDisponivel);
    }
}
