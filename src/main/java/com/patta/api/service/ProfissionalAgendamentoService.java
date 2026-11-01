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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        Map<String, String> tutores = userRepository.findAll().stream()
                .collect(Collectors.toMap(
                        usuario -> usuario.getId(),
                        usuario -> usuario.getNome() == null ? "" : usuario.getNome(),
                        (first, ignored) -> first));
        Map<String, String> pets = animalRepository.findAll().stream()
                .collect(Collectors.toMap(
                        animal -> animal.getId(),
                        animal -> animal.getNome() == null ? "" : animal.getNome(),
                        (first, ignored) -> first));

        return agendamentos.stream()
                .filter(agendamento -> papelUsuarioService.ehAdministrador(uid)
                        || "USER".equals(papelUsuarioService.buscarPapel(agendamento.usuarioId())))
                .map(agendamento -> toResponse(agendamento,
                        tutores.getOrDefault(agendamento.usuarioId(), "Tutor"),
                        pets.getOrDefault(agendamento.animalId(), "Pet")))
                .toList();
    }

    public ProfissionalAgendamentoResponseDTO buscar(String uid, AgendamentoResponseDTO agendamento)
            throws ExecutionException, InterruptedException {
        if (!papelUsuarioService.ehAdministrador(uid)
                && !"USER".equals(papelUsuarioService.buscarPapel(agendamento.usuarioId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado.");
        }
        String tutorNome = userRepository.findById(agendamento.usuarioId())
                .map(usuario -> usuario.getNome() == null ? "" : usuario.getNome())
                .orElse("Tutor");
        String petNome = animalRepository.findById(agendamento.animalId())
                .map(animal -> animal.getNome() == null ? "" : animal.getNome())
                .orElse("Pet");
        return toResponse(agendamento, tutorNome, petNome);
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
                hospedagem, checkInDisponivel, checkOutDisponivel, atendimentoDisponivel);
    }
}
