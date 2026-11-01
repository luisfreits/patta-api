package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.patta.api.model.VacinaModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class VacinaRepository {

    private static final String COLECAO = "vacinas";
    private static final int LIMITE_LOTE = 500;
    private final Firestore firestore;

    public VacinaModel save(VacinaModel vacina) throws ExecutionException, InterruptedException {
        DocumentReference documento = vacina.getId() == null
                ? vacinas().document()
                : vacinas().document(vacina.getId());
        vacina.setId(documento.getId());
        documento.set(vacina).get();
        return vacina;
    }

    public Optional<VacinaModel> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot documento = vacinas().document(id).get().get();
        return documento.exists() ? Optional.ofNullable(toModel(documento)) : Optional.empty();
    }

    public List<VacinaModel> findByAnimalId(String animalId) throws ExecutionException, InterruptedException {
        return vacinas().whereEqualTo("animalId", animalId).get().get().getDocuments().stream()
                .map(this::toModel)
                .sorted(Comparator.comparing(VacinaModel::getAplicadaEm,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public void deleteByAnimalId(String animalId) throws ExecutionException, InterruptedException {
        delete("animalId", animalId);
    }

    public void deleteByUsuarioId(String usuarioId) throws ExecutionException, InterruptedException {
        delete("usuarioId", usuarioId);
    }

    private void delete(String campo, String valor) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> documentos = vacinas().whereEqualTo(campo, valor)
                .get().get().getDocuments();
        for (int inicio = 0; inicio < documentos.size(); inicio += LIMITE_LOTE) {
            WriteBatch lote = firestore.batch();
            documentos.subList(inicio, Math.min(inicio + LIMITE_LOTE, documentos.size()))
                    .forEach(documento -> lote.delete(documento.getReference()));
            lote.commit().get();
        }
    }

    private CollectionReference vacinas() {
        return firestore.collection(COLECAO);
    }

    private VacinaModel toModel(DocumentSnapshot documento) {
        VacinaModel model = documento.toObject(VacinaModel.class);
        if (model != null) model.setId(documento.getId());
        return model;
    }
}
