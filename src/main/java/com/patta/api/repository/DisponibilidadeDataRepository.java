package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.patta.api.model.DisponibilidadeDataModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class DisponibilidadeDataRepository {

    private static final String COLECAO = "disponibilidadesPorData";
    private final Firestore firestore;

    public Optional<DisponibilidadeDataModel> findByServicoAndData(String servico, LocalDate data)
            throws ExecutionException, InterruptedException {
        var snapshot = disponibilidades().document(id(servico, data)).get().get();
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        DisponibilidadeDataModel model = snapshot.toObject(DisponibilidadeDataModel.class);
        if (model != null) {
            model.setId(snapshot.getId());
        }
        return Optional.ofNullable(model);
    }

    public void saveAll(List<DisponibilidadeDataModel> disponibilidades)
            throws ExecutionException, InterruptedException {
        WriteBatch batch = firestore.batch();
        for (DisponibilidadeDataModel disponibilidade : disponibilidades) {
            DocumentReference documento = disponibilidades().document(disponibilidade.getId());
            batch.set(documento, disponibilidade);
        }
        batch.commit().get();
    }

    public void deleteRange(String servico, LocalDate inicio, LocalDate fim)
            throws ExecutionException, InterruptedException {
        WriteBatch batch = firestore.batch();
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            batch.delete(disponibilidades().document(id(servico, data)));
        }
        batch.commit().get();
    }

    public static String id(String servico, LocalDate data) {
        return servico + "_" + data;
    }

    private CollectionReference disponibilidades() {
        return firestore.collection(COLECAO);
    }
}
