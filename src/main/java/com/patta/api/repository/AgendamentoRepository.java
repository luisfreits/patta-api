package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.WriteBatch;
import com.patta.api.model.AgendamentoModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public AgendamentoModel saveIfAvailable(AgendamentoModel agendamento, int capacidade)
            throws ExecutionException, InterruptedException {
        DocumentReference documento = agendamentos().document();
        DocumentReference trava = firestore.collection("travasReservas").document(agendamento.getServico());
        agendamento.setId(documento.getId());
        try {
            firestore.runTransaction(transaction -> {
                DocumentSnapshot estadoTrava = transaction.get(trava).get();
                Query consulta = agendamentos().whereEqualTo("servico", agendamento.getServico());
                List<QueryDocumentSnapshot> existentes = transaction.get(consulta).get().getDocuments();
                int reservasConflitantes = 0;
                for (QueryDocumentSnapshot existente : existentes) {
                    AgendamentoModel reserva = toModel(existente);
                    if (reserva != null && reservaAtiva(reserva) && conflita(agendamento, reserva)) {
                        reservasConflitantes++;
                    }
                }
                if (reservasConflitantes >= capacidade) {
                    throw new ConflitoReservaException();
                }
                long revisao = estadoTrava.exists() && estadoTrava.getLong("revisao") != null
                        ? estadoTrava.getLong("revisao")
                        : 0L;
                transaction.set(trava, Map.of("revisao", revisao + 1));
                transaction.set(documento, agendamento);
                return null;
            }).get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof ConflitoReservaException conflito) throw conflito;
            throw exception;
        }
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

    public List<AgendamentoModel> findByServico(String servico)
            throws ExecutionException, InterruptedException {
        return agendamentos().whereEqualTo("servico", servico).get().get().getDocuments().stream()
                .map(this::toModel)
                .filter(Objects::nonNull)
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

    private boolean reservaAtiva(AgendamentoModel agendamento) {
        return !"REJEITADO".equals(agendamento.getStatus())
                && !"CANCELADO".equals(agendamento.getStatus())
                && agendamento.getCheckOutEm() == null;
    }

    private boolean conflita(AgendamentoModel nova, AgendamentoModel existente) {
        if ("hospedagem".equals(nova.getServico())) {
            LocalDate inicioNovo = LocalDate.parse(nova.getData());
            LocalDate fimNovo = LocalDate.parse(nova.getDataSaidaPrevista());
            LocalDate inicioExistente = LocalDate.parse(existente.getData());
            LocalDate fimExistente = existente.getDataSaidaPrevista() == null
                    ? inicioExistente.plusDays(1)
                    : LocalDate.parse(existente.getDataSaidaPrevista());
            return inicioNovo.isBefore(fimExistente) && inicioExistente.isBefore(fimNovo);
        }

        LocalDateTime inicioNovo = LocalDateTime.of(
                LocalDate.parse(nova.getData()), LocalTime.parse(nova.getHorario()));
        LocalDateTime fimNovo = inicioNovo.plusMinutes(nova.getDuracaoMinutos());
        LocalDateTime inicioExistente = LocalDateTime.of(
                LocalDate.parse(existente.getData()), LocalTime.parse(existente.getHorario()));
        LocalDateTime fimExistente = inicioExistente.plusMinutes(
                existente.getDuracaoMinutos() == null ? nova.getDuracaoMinutos() : existente.getDuracaoMinutos());
        return inicioNovo.isBefore(fimExistente) && inicioExistente.isBefore(fimNovo);
    }

    public static final class ConflitoReservaException extends RuntimeException {
    }
}
