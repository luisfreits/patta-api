package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.patta.api.dto.UserResponseDTO;
import com.patta.api.dto.UserUpdateDTO;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;

/** Regras de perfil: toda operação recebe o UID que já foi validado pelo filtro Firebase. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final FirebaseAuth firebaseAuth;

    public UserResponseDTO buscarMeuPerfil(String authenticatedUid)
            throws ExecutionException, InterruptedException {
        return toResponse(findUser(authenticatedUid));
    }

    public UserResponseDTO atualizarMeuPerfil(String authenticatedUid, UserUpdateDTO update)
            throws ExecutionException, InterruptedException {
        UserModel user = findUser(authenticatedUid);
        user.setNome(update.getNome().trim());
        user.setTelefone(update.getTelefone().trim());
        // E-mail e id não são alterados pelo endpoint de perfil.
        return toResponse(userRepository.save(user));
    }

    public void deletarMinhaConta(String authenticatedUid)
            throws ExecutionException, InterruptedException {
        findUser(authenticatedUid);
        // Apaga primeiro os documentos dependentes; Firestore batch limita cada lote a 500 operações.
        animalRepository.deleteByUserId(authenticatedUid);
        userRepository.deleteById(authenticatedUid);
        try {
            firebaseAuth.deleteUser(authenticatedUid);
        } catch (FirebaseAuthException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Os dados foram removidos, mas não foi possível excluir a conta no Firebase Authentication.");
        }
    }

    private UserModel findUser(String authenticatedUid)
            throws ExecutionException, InterruptedException {
        return userRepository.findById(authenticatedUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private UserResponseDTO toResponse(UserModel user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .nome(user.getNome())
                .email(user.getEmail())
                .telefone(user.getTelefone())
                .build();
    }
}