package com.patta.api.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.patta.api.model.AnimalModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Acesso aos documentos da coleção de animais no Firestore. */
@Repository
@RequiredArgsConstructor
public class AnimalRepository {

    private static final String COLLECTION = "animais";
    private static final int FIRESTORE_BATCH_LIMIT = 500;

    // O construtor gerado pelo Lombok recebe o mesmo Firestore configurado pela aplicação.
    private final Firestore firestore;

    /** Salva um animal novo ou substitui o documento cujo id já foi informado. */
    public AnimalModel save(AnimalModel animal) throws ExecutionException, InterruptedException {
        DocumentReference document;
        if (animal.getId() == null || animal.getId().isBlank()) {
            document = firestore.collection(COLLECTION).document();
            animal.setId(document.getId());
        } else {
            document = firestore.collection(COLLECTION).document(animal.getId());
        }
        document.set(animal).get();
        return animal;
    }

    /** Consulta somente os documentos cujo campo userId corresponde ao UID autenticado. */
    public List<AnimalModel> findByUserId(String userId) throws ExecutionException, InterruptedException {
        QuerySnapshot result = firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .get();
        List<AnimalModel> animals = new ArrayList<>();
        for (QueryDocumentSnapshot document : result.getDocuments()) {
            AnimalModel animal = document.toObject(AnimalModel.class);
            animal.setId(document.getId());
            animals.add(animal);
        }
        return animals;
    }

    /** Busca pelo id do documento e mantém o id do Firestore no model retornado. */
    public Optional<AnimalModel> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(COLLECTION).document(id).get().get();
        if (!document.exists()) {
            return Optional.empty();
        }
        AnimalModel animal = document.toObject(AnimalModel.class);
        if (animal != null) {
            animal.setId(document.getId());
        }
        return Optional.ofNullable(animal);
    }

    /** Exclui um documento pelo id; a autorização de propriedade pertence ao service. */
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION).document(id).delete().get();
    }

    /** Exclui todos os animais do usuário sem exceder o limite de 500 operações por lote. */
    public void deleteByUserId(String userId) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get();
        List<QueryDocumentSnapshot> documents = query.get().getDocuments();
        for (int start = 0; start < documents.size(); start += FIRESTORE_BATCH_LIMIT) {
            List<QueryDocumentSnapshot> chunk = documents.subList(
                    start, Math.min(start + FIRESTORE_BATCH_LIMIT, documents.size()));
            var batch = firestore.batch();
            for (QueryDocumentSnapshot document : chunk) {
                batch.delete(document.getReference());
            }
            batch.commit().get();
        }
    }
}