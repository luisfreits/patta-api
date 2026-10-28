package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.patta.api.dto.UserResponseDTO;
import com.patta.api.dto.UserUpdateDTO;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;

/**
 * Regras do perfil do usuário. Todo método recebe o uid já validado pelo filtro do Firebase.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final FirebaseAuth firebaseAuth;

    /** Devolve o perfil do usuário logado. */
    public UserResponseDTO buscarMeuPerfil(String uid) throws ExecutionException, InterruptedException {
        return toResponse(buscarUsuario(uid));
    }

    /** Atualiza nome e telefone. E-mail e uid não mudam por este endpoint. */
    public UserResponseDTO atualizarMeuPerfil(String uid, UserUpdateDTO dados)
            throws ExecutionException, InterruptedException {
        UserModel usuario = buscarUsuario(uid);
        usuario.setNome(dados.nome().trim());
        usuario.setTelefone(dados.telefone().trim());
        return toResponse(userRepository.save(usuario));
    }

    /**
     * Apaga a conta: primeiro os animais e o documento do usuário no Firestore,
     * depois o usuário no Firebase Auth. Se o Firebase Auth falhar, os dados já foram
     * apagados, então o erro 502 informa que a conta precisa ser removida novamente.
     */
    public void deletarMinhaConta(String uid) throws ExecutionException, InterruptedException {
        buscarUsuario(uid); // Confirma que o usuário existe antes de apagar qualquer coisa
        // O repository divide a exclusão em lotes quando há muitos documentos (limite de 500 operações por batch no Firestore)
        animalRepository.deleteByUserId(uid);
        agendamentoRepository.deleteByUsuarioId(uid);
        userRepository.deleteById(uid);
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Os dados foram removidos, mas não foi possível excluir a conta no Firebase Authentication.");
        }
    }

    // Busca o usuário pelo uid; 404 se o documento não existir no Firestore
    private UserModel buscarUsuario(String uid) throws ExecutionException, InterruptedException {
        return userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    // Converte a entidade em DTO, sem expor nada além dos campos públicos
    private UserResponseDTO toResponse(UserModel usuario) {
        return UserResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .build();
    }
}
