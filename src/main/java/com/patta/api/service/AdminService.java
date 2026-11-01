package com.patta.api.service;

import com.patta.api.dto.AdminAnimalResponseDTO;
import com.patta.api.dto.AdminAgendamentoRequestDTO;
import com.patta.api.dto.AdminUserResponseDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.UserUpdateDTO;
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
    private final UserService userService;

    public List<AdminUserResponseDTO> listarUsuarios(String uid)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    public AdminUserResponseDTO atualizarUsuario(String administradorUid, String usuarioUid, UserUpdateDTO dados)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(administradorUid);
        userService.atualizarPerfilComoAdministrador(administradorUid, usuarioUid, dados);
        return buscarUsuarioAtualizado(administradorUid, usuarioUid);
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

    public AgendamentoResponseDTO marcarCheckIn(String uid, String agendamentoId)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        return agendamentoService.marcarCheckIn(uid, agendamentoId);
    }

    public AgendamentoResponseDTO marcarCheckOut(String uid, String agendamentoId)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(uid);
        return agendamentoService.marcarCheckOut(uid, agendamentoId);
    }

    private void exigirAdministrador(String uid) {
        if (!papelUsuarioService.ehAdministrador(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores autorizados podem consultar estes dados.");
        }
    }

    private AdminUserResponseDTO buscarUsuarioAtualizado(String administradorUid, String usuarioUid)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(administradorUid);
        UserModel usuario = userRepository.findById(usuarioUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return toUserResponse(usuario);
    }

    private AdminUserResponseDTO toUserResponse(UserModel usuario) {
        return new AdminUserResponseDTO(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTelefone(),
                usuario.getFotoPerfil(),
                papelUsuarioService.buscarPapel(usuario.getId()));
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
