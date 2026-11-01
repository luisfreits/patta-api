package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.patta.api.model.RegistroClinicoModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class RegistroClinicoRepository {

    private static final String COLECAO = "historicoClinico";
    private static final int LIMITE_LOTE = 500;
    private final Firestore firestore;

    public RegistroClinicoModel save(RegistroClinicoModel registro)
            throws ExecutionException, InterruptedException {
        DocumentReference documento = registro.getId() == null
                ? registros().document()
                : registros().document(registro.getId());
        registro.setId(documento.getId());
        documento.set(registro).get();
        return registro;
    }

    public Optional<RegistroClinicoModel> findById(String id)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot documento = registros().document(id).get().get();
        return documento.exists() ? Optional.ofNullable(toModel(documento)) : Optional.empty();
    }

    public List<RegistroClinicoModel> findByAnimalId(String animalId)
            throws ExecutionException, InterruptedException {
        return registros().whereEqualTo("animalId", animalId).get().get().getDocuments().stream()
                .map(this::toModel)
                .sorted(Comparator.comparing(RegistroClinicoModel::getDataAtendimento,
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
        List<QueryDocumentSnapshot> documentos = registros().whereEqualTo(campo, valor)
                .get().get().getDocuments();
        for (int inicio = 0; inicio < documentos.size(); inicio += LIMITE_LOTE) {
            WriteBatch lote = firestore.batch();
            documentos.subList(inicio, Math.min(inicio + LIMITE_LOTE, documentos.size()))
                    .forEach(documento -> lote.delete(documento.getReference()));
            lote.commit().get();
        }
    }

    private CollectionReference registros() {
        return firestore.collection(COLECAO);
    }

    private RegistroClinicoModel toModel(DocumentSnapshot documento) {
        RegistroClinicoModel model = documento.toObject(RegistroClinicoModel.class);
        if (model != null) model.setId(documento.getId());
        return model;
    }
}
