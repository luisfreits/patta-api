package com.patta.api.service;

import com.patta.api.dto.AdminAnimalResponseDTO;
import com.patta.api.dto.AdminAgendamentoRequestDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.UserResponseDTO;
import com.patta.api.model.AnimalModel;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final PapelUsuarioService papelUsuarioService;
    private final AgendamentoService agendamentoService;

    public List<UserResponseDTO> listarUsuarios(String uid)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    public List<AdminAnimalResponseDTO> listarAnimais(String uid)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        return animalRepository.findAll().stream()
                .map(this::toAnimalResponse)
                .toList();
    }

    public List<AgendamentoResponseDTO> listarAgendamentos(String uid)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        return agendamentoService.listarTodos(uid);
    }

    public AgendamentoResponseDTO criarAgendamento(String uid, AdminAgendamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        userRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return agendamentoService.criarParaUsuario(uid, request.usuarioId(), request.agendamento());
    }

    private void exigirAdministrador(String uid) {
        if (!papelUsuarioService.ehAdministrador(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores autorizados podem consultar estes dados.");
        }
    }

    private UserResponseDTO toUserResponse(UserModel usuario) {
        return UserResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .build();
    }

    private AdminAnimalResponseDTO toAnimalResponse(AnimalModel animal) {
        return new AdminAnimalResponseDTO(
                animal.getId(),
                animal.getUserId(),
                animal.getNome(),
                animal.getSexo(),
                animal.getIdade(),
                animal.getAnimal(),
                animal.getCastrado());
    }
}
