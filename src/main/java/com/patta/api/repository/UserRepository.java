package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.patta.api.model.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Acesso à coleção "users" no Firestore.
 * Cada documento tem como id o uid do Firebase Authentication.
 */
@Repository
@RequiredArgsConstructor // Injeta o Firestore pelo construtor
public class UserRepository {

    private static final String COLECAO = "users";

    private final Firestore firestore;

    /** Referência à coleção de usuários. */
    private CollectionReference usuarios() {
        return firestore.collection(COLECAO);
    }

    /**
     * Cria ou sobrescreve o documento users/{uid}.
     * O id é obrigatório, pois é o uid do Firebase; sem ele não há como saber qual documento usar.
     */
    public UserModel save(UserModel usuario) throws ExecutionException, InterruptedException {
        if (usuario.getId() == null || usuario.getId().isBlank()) {
            throw new IllegalArgumentException("O usuário precisa do uid do Firebase para ser salvo.");
        }
        usuarios().document(usuario.getId()).set(usuario).get(); // get() aguarda a confirmação da gravação
        return usuario;
    }

    /** Busca o usuário pelo uid. Retorna vazio se o documento não existir. */
    public Optional<UserModel> findById(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot documento = usuarios().document(uid).get().get();
        if (!documento.exists()) {
            return Optional.empty();
        }
        UserModel usuario = documento.toObject(UserModel.class);
        if (usuario != null) {
            usuario.setId(documento.getId());
        }
        return Optional.ofNullable(usuario);
    }

    /** Lista os perfis cadastrados, ordenados por nome e UID. */
    public List<UserModel> findAll() throws ExecutionException, InterruptedException {
        return usuarios().get().get().getDocuments().stream()
                .map(documento -> {
                    UserModel usuario = documento.toObject(UserModel.class);
                    if (usuario != null) {
                        usuario.setId(documento.getId());
                    }
                    return usuario;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                                UserModel::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UserModel::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Apaga o documento do usuário. A conta no Firebase Auth é removida pelo UserService. */
    public void deleteById(String uid) throws ExecutionException, InterruptedException {
        usuarios().document(uid).delete().get();
    }
}
