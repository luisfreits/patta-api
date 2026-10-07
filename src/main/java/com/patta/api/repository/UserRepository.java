package com.patta.api.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.patta.api.model.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor

public class UserRepository {

    private static final String COLLECTION = "users";

    private final Firestore firestore;

    // --- CRIAR / ATUALIZAR USUÁRIO ---
    public UserModel save(UserModel userModel) throws ExecutionException, InterruptedException {
        DocumentReference docRef;

        // Se o objeto não tiver ID, gera um novo ID de documento automaticamente no Firestore
        if (userModel.getId() == null || userModel.getId().isBlank()) {
            docRef = firestore.collection(COLLECTION).document();
            userModel.setId(docRef.getId());
        } else {
            // Se já possui ID, referencia o documento existente para sobrescrevê-lo
            docRef = firestore.collection(COLLECTION).document(userModel.getId());
        }

        // Persiste os dados de forma assíncrona, mas aguarda a confirmação com o .get()
        docRef.set(userModel).get();
        return userModel;
    }

    // --- BUSCAR USUÁRIO POR ID ---
    public Optional<UserModel> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection(COLLECTION)
                .document(id)
                .get()
                .get();

        if (!doc.exists()) {
            return Optional.empty();
        }

        UserModel userModel = doc.toObject(UserModel.class);
        if (userModel != null) userModel.setId(doc.getId());
        return Optional.ofNullable(userModel);
    }

    // --- REMOVER USUÁRIO POR ID ---
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION).document(id).delete().get();
    }
}