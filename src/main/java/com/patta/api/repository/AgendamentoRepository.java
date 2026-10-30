package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.patta.api.model.AgendamentoModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class AgendamentoRepository {

    private static final String COLECAO = "agendamentos";
    private static final String CAMPO_USUARIO_ID = "usuarioId";
    private static final int LIMITE_LOTE = 500;
    private static final Comparator<AgendamentoModel> ORDENACAO = Comparator
            .comparing(AgendamentoModel::getData, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AgendamentoModel::getHorario, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AgendamentoModel::getCriadoEm, Comparator.nullsLast(Comparator.reverseOrder()));

    private final Firestore firestore;

    public AgendamentoModel save(AgendamentoModel agendamento)
            throws ExecutionException, InterruptedException {
        DocumentReference documento = agendamento.getId() == null || agendamento.getId().isBlank()
                ? agendamentos().document()
                : agendamentos().document(agendamento.getId());
        agendamento.setId(documento.getId());
        documento.set(agendamento).get();
        return agendamento;
    }

    public Optional<AgendamentoModel> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot documento = agendamentos().document(id).get().get();
        return documento.exists() ? Optional.ofNullable(toModel(documento)) : Optional.empty();
    }

    public List<AgendamentoModel> findByUsuarioId(String usuarioId)
            throws ExecutionException, InterruptedException {
        return agendamentos().whereEqualTo(CAMPO_USUARIO_ID, usuarioId).get().get().getDocuments().stream()
                .map(this::toModel)
                .filter(Objects::nonNull)
                .sorted(ORDENACAO)
                .toList();
    }

    public List<AgendamentoModel> findAll()
            throws ExecutionException, InterruptedException {
        return agendamentos().get().get().getDocuments().stream()
                .map(this::toModel)
                .filter(Objects::nonNull)
                .sorted(ORDENACAO)
                .toList();
    }

    public void deleteByUsuarioId(String usuarioId) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> documentos = agendamentos()
                .whereEqualTo(CAMPO_USUARIO_ID, usuarioId).get().get().getDocuments();
        for (int inicio = 0; inicio < documentos.size(); inicio += LIMITE_LOTE) {
            WriteBatch lote = firestore.batch();
            documentos.subList(inicio, Math.min(inicio + LIMITE_LOTE, documentos.size()))
                    .forEach(documento -> lote.delete(documento.getReference()));
            lote.commit().get();
        }
    }

    private CollectionReference agendamentos() {
        return firestore.collection(COLECAO);
    }

    private AgendamentoModel toModel(DocumentSnapshot documento) {
        AgendamentoModel agendamento = documento.toObject(AgendamentoModel.class);
        if (agendamento != null) {
            agendamento.setId(documento.getId());
        }
        return agendamento;
    }
}
