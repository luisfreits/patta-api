package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.patta.api.model.AgendamentoModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class AgendamentoRepository {

    private static final String COLECAO = "agendamentos";
    private static final String COLECAO_CONFIGURACAO = "configuracao";
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
        DocumentReference configClinica = firestore.collection(COLECAO_CONFIGURACAO).document("clinica");
        boolean hospedagem = "hospedagem".equals(agendamento.getServico());
        agendamento.setId(documento.getId());
        try {
            firestore.runTransaction(transaction -> {
                DocumentSnapshot estadoTrava = transaction.get(trava).get();
                Query consulta = agendamentos().whereEqualTo("servico", agendamento.getServico());
                List<QueryDocumentSnapshot> existentes = transaction.get(consulta).get().getDocuments();
                Integer capacidadeHospedagem = null;
                if (hospedagem) {
                    DocumentSnapshot config = transaction.get(configClinica).get();
                    Long capacidadeArmazenada = config.exists()
                            ? config.getLong("capacidadeHospedagem")
                            : null;
                    capacidadeHospedagem = capacidadeArmazenada == null
                            ? null
                            : Math.toIntExact(capacidadeArmazenada);
                }

                int reservasConflitantes = 0;
                List<AgendamentoModel> hospedagensAtivas = new ArrayList<>();
                for (QueryDocumentSnapshot existente : existentes) {
                    AgendamentoModel reserva = toModel(existente);
                    if (reserva != null && mesmoAgendamento(agendamento, reserva)) {
                        throw new AgendamentoDuplicadoException();
                    }
                    if (reserva != null && reservaAtiva(reserva)) {
                        if (hospedagem) {
                            hospedagensAtivas.add(reserva);
                        } else if (conflita(agendamento, reserva)) {
                            reservasConflitantes++;
                        }
                    }
                }
                if (hospedagem) {
                    LocalDateTime inicio = inicio(agendamento);
                    LocalDateTime fim = saidaPrevista(agendamento, inicio);
                    if (capacidadeHospedagem == null || capacidadeHospedagem <= 0
                            || picoOcupacao(hospedagensAtivas, inicio, fim) >= capacidadeHospedagem) {
                        throw new ConflitoReservaException();
                    }
                } else if (reservasConflitantes >= capacidade) {
                    throw new ConflitoReservaException();
                }
                long revisao = estadoTrava.exists() && estadoTrava.getLong("revisao") != null
                        ? estadoTrava.getLong("revisao")
                        : 0L;
                // Ler e incrementar esta revisão compartilhada força transações concorrentes
                // do mesmo serviço a repetir a leitura com a ocupação atualizada.
                transaction.set(trava, Map.of("revisao", revisao + 1));
                transaction.set(documento, agendamento);
                return null;
            }).get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof AgendamentoDuplicadoException duplicado) throw duplicado;
            if (exception.getCause() instanceof ConflitoReservaException conflito) throw conflito;
            throw exception;
        }
        return agendamento;
    }

    public int picoOcupacaoHospedagem(LocalDateTime inicio, LocalDateTime fim)
            throws ExecutionException, InterruptedException {
        return calcularPicoOcupacaoHospedagem(findByServico("hospedagem"), inicio, fim);
    }

    static int calcularPicoOcupacaoHospedagem(List<AgendamentoModel> reservas,
                                               LocalDateTime inicio, LocalDateTime fim) {
        return picoOcupacao(reservas.stream()
                .filter(AgendamentoRepository::reservaAtiva)
                .toList(), inicio, fim);
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

    private static boolean reservaAtiva(AgendamentoModel agendamento) {
        return ("PENDENTE".equals(agendamento.getStatus())
                || "APROVADO".equals(agendamento.getStatus()))
                && agendamento.getCheckOutEm() == null;
    }

    private boolean mesmoAgendamento(AgendamentoModel novo, AgendamentoModel existente) {
        if ("REJEITADO".equals(existente.getStatus()) || "CANCELADO".equals(existente.getStatus())) {
            return false;
        }
        return Objects.equals(novo.getAnimalId(), existente.getAnimalId())
                && Objects.equals(novo.getServico(), existente.getServico())
                && Objects.equals(LocalDate.parse(novo.getData()), LocalDate.parse(existente.getData()))
                && Objects.equals(LocalTime.parse(novo.getHorario()), LocalTime.parse(existente.getHorario()));
    }

    private boolean conflita(AgendamentoModel nova, AgendamentoModel existente) {
        if ("hospedagem".equals(nova.getServico())) {
            LocalDateTime inicioNovo = inicio(nova);
            LocalDateTime inicioExistente = inicio(existente);
            return inicioNovo.isBefore(saidaPrevista(existente, inicioExistente))
                    && inicioExistente.isBefore(saidaPrevista(nova, inicioNovo));
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

    private static int picoOcupacao(List<AgendamentoModel> reservas, LocalDateTime inicio, LocalDateTime fim) {
        List<EventoOcupacao> eventos = new ArrayList<>();
        for (AgendamentoModel reserva : reservas) {
            LocalDateTime inicioReserva = inicio(reserva);
            LocalDateTime fimReserva = reserva.getCheckInEm() != null && reserva.getCheckOutEm() == null
                    ? LocalDateTime.MAX
                    : saidaPrevista(reserva, inicioReserva);
            LocalDateTime inicioSobreposto = inicioReserva.isAfter(inicio) ? inicioReserva : inicio;
            LocalDateTime fimSobreposto = fimReserva.isBefore(fim) ? fimReserva : fim;
            if (inicioSobreposto.isBefore(fimSobreposto)) {
                eventos.add(new EventoOcupacao(inicioSobreposto, 1));
                eventos.add(new EventoOcupacao(fimSobreposto, -1));
            }
        }
        eventos.sort(Comparator.comparing(EventoOcupacao::instante)
                .thenComparingInt(EventoOcupacao::alteracao));
        int ocupacao = 0;
        int pico = 0;
        for (EventoOcupacao evento : eventos) {
            ocupacao += evento.alteracao();
            pico = Math.max(pico, ocupacao);
        }
        return pico;
    }

    private static LocalDateTime inicio(AgendamentoModel agendamento) {
        LocalDate data = LocalDate.parse(agendamento.getData());
        LocalTime horario = agendamento.getHorario() == null
                ? LocalTime.MIDNIGHT
                : LocalTime.parse(agendamento.getHorario());
        return LocalDateTime.of(data, horario);
    }

    private static LocalDateTime saidaPrevista(AgendamentoModel agendamento, LocalDateTime inicio) {
        if (agendamento.getDataSaidaPrevista() == null) {
            return inicio.plusDays(agendamento.getTempoHospedagemDias() == null
                    ? 1
                    : agendamento.getTempoHospedagemDias());
        }
        try {
            return LocalDateTime.parse(agendamento.getDataSaidaPrevista());
        } catch (DateTimeParseException exception) {
            return LocalDate.parse(agendamento.getDataSaidaPrevista()).atTime(inicio.toLocalTime());
        }
    }

    private record EventoOcupacao(LocalDateTime instante, int alteracao) {}

    public static final class ConflitoReservaException extends RuntimeException {
    }

    public static final class AgendamentoDuplicadoException extends RuntimeException {
    }
}
