package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.patta.api.model.AnimalModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Acesso à coleção "animais" no Firestore.
 * Não confere propriedade: essa regra fica no AnimalService.
 */
@Repository
@RequiredArgsConstructor // Injeta o Firestore pelo construtor
public class AnimalRepository {

    private static final String COLECAO = "animais";
    private static final String CAMPO_USER_ID = "userId";
    private static final int LIMITE_LOTE = 500; // Máximo de operações por batch no Firestore

    private final Firestore firestore;

    /** Referência à coleção de animais. */
    private CollectionReference animais() {
        return firestore.collection(COLECAO);
    }

    /**
     * Cria um documento novo quando o id está vazio, ou sobrescreve o existente.
     * Retorna o model com o id definido.
     */
    public AnimalModel save(AnimalModel animal) throws ExecutionException, InterruptedException {
        DocumentReference documento = (animal.getId() == null || animal.getId().isBlank())
                ? animais().document()             // Id gerado pelo Firestore
                : animais().document(animal.getId());
        animal.setId(documento.getId());
        documento.set(animal).get();               // get() aguarda a confirmação da gravação
        return animal;
    }

    /** Lista os animais de um dono. */
    public List<AnimalModel> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return consultarPorUserId(userId).stream()
                .map(this::toModel)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Busca um animal pelo id do documento. */
    public Optional<AnimalModel> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot documento = animais().document(id).get().get();
        return documento.exists() ? Optional.ofNullable(toModel(documento)) : Optional.empty();
    }

    /** Apaga um animal pelo id. A checagem de dono é feita antes, no service. */
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        animais().document(id).delete().get();
    }

    /**
     * Apaga todos os animais de um dono em lotes de até 500 operações,
     * o máximo permitido pelo batch do Firestore.
     */
    public void deleteByUserId(String userId) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> documentos = consultarPorUserId(userId);
        for (int inicio = 0; inicio < documentos.size(); inicio += LIMITE_LOTE) {
            int fim = Math.min(inicio + LIMITE_LOTE, documentos.size());
            WriteBatch lote = firestore.batch();
            for (QueryDocumentSnapshot documento : documentos.subList(inicio, fim)) {
                lote.delete(documento.getReference());
            }
            lote.commit().get();
        }
    }

    // Consulta os documentos cujo userId é igual ao informado. Usada por leitura e exclusão.
    private List<QueryDocumentSnapshot> consultarPorUserId(String userId)
            throws ExecutionException, InterruptedException {
        return animais().whereEqualTo(CAMPO_USER_ID, userId).get().get().getDocuments();
    }

    // Converte o documento em AnimalModel e preenche o id com o id do Firestore.
    // Retorna null se o documento não puder ser convertido.
    private AnimalModel toModel(DocumentSnapshot documento) {
        AnimalModel animal = documento.toObject(AnimalModel.class);
        if (animal != null) {
            animal.setId(documento.getId());
        }
        return animal;
    }
}
