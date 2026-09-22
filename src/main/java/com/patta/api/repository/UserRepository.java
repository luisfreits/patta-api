package com.patta.api.repository;


import com.patta.api.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor

public class UserRepository {

    private static final String COLLECTION = "users";

    private final Firestore firestore;

    // --- CRIAR / ATUALIZAR USUÁRIO ---
    public User save(User user) throws ExecutionException, InterruptedException {
        DocumentReference docRef;

        // Se o objeto não tiver ID, gera um novo ID de documento automaticamente no Firestore
        if (user.getId() == null || user.getId().isBlank()) {
            docRef = firestore.collection(COLLECTION).document();
            user.setId(docRef.getId());
        } else {
            // Se já possui ID, referencia o documento existente para sobrescrevê-lo
            docRef = firestore.collection(COLLECTION).document(user.getId());
        }

        // Persiste os dados de forma assíncrona, mas aguarda a confirmação com o .get()
        docRef.set(user).get();
        return user;
    }

    // --- BUSCAR USUÁRIO POR ID ---
    public Optional<User> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection(COLLECTION)
                .document(id)
                .get()
                .get();

        if (!doc.exists()) {
            return Optional.empty();
        }

        User user = doc.toObject(User.class);
        if (user != null) user.setId(doc.getId());
        return Optional.ofNullable(user);
    }

    // --- LISTAR TODOS OS USUÁRIOS ---
    public List<User> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION).get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        List<User> users = new ArrayList<>();
        for (QueryDocumentSnapshot doc : docs) {
            User user = doc.toObject(User.class);
            user.setId(doc.getId());
            users.add(user);
        }
        return users;
    }

    // --- REMOVER USUÁRIO POR ID ---
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION).document(id).delete().get();
    }
}