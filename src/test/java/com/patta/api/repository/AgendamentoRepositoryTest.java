package com.patta.api.repository;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import com.patta.api.model.AgendamentoModel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgendamentoRepositoryTest {

    @Test
    void calculaPicoSemSomarReservasEmTrechosDisjuntos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 9, 0);
        List<AgendamentoModel> reservas = List.of(
                hospedagem("a", inicio, inicio.plusHours(2)),
                hospedagem("b", inicio.plusHours(4), inicio.plusHours(6)));

        assertEquals(1, AgendamentoRepository.calcularPicoOcupacaoHospedagem(
                reservas, inicio, inicio.plusHours(8)));
    }

    @Test
    void calculaPicoDeOcupacaoQuandoReservasSeSobrepoem() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 9, 0);
        List<AgendamentoModel> reservas = List.of(
                hospedagem("a", inicio, inicio.plusHours(4)),
                hospedagem("b", inicio.plusHours(2), inicio.plusHours(5)));

        assertEquals(2, AgendamentoRepository.calcularPicoOcupacaoHospedagem(
                reservas, inicio, inicio.plusHours(8)));
    }

    @Test
    void checkoutECancelamentoLiberamVagasMasCheckInSemCheckoutMantemOcupacao() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 9, 0);
        AgendamentoModel cancelada = hospedagem("cancelada", inicio, inicio.plusDays(1));
        cancelada.setStatus("CANCELADO");
        AgendamentoModel rejeitada = hospedagem("rejeitada", inicio, inicio.plusDays(1));
        rejeitada.setStatus("REJEITADO");
        AgendamentoModel checkout = hospedagem("checkout", inicio, inicio.plusDays(1));
        checkout.setStatus("APROVADO");
        checkout.setCheckOutEm(new Date());
        AgendamentoModel emAndamento = hospedagem("em-andamento", inicio, inicio.plusDays(1));
        emAndamento.setCheckInEm(new Date());

        assertEquals(1, AgendamentoRepository.calcularPicoOcupacaoHospedagem(
                List.of(cancelada, rejeitada, checkout, emAndamento),
                inicio.plusDays(3), inicio.plusDays(3).plusHours(1)));
        assertEquals(0, AgendamentoRepository.calcularPicoOcupacaoHospedagem(
                List.of(cancelada, rejeitada, checkout),
                inicio, inicio.plusDays(1)));
    }

    @Test
    void interpretaDataDeSaidaLegadaComoDuracaoEntreEntradaESaida() {
        LocalDate entrada = LocalDate.of(2026, 11, 10);
        AgendamentoModel legado = AgendamentoModel.builder()
                .servico("hospedagem")
                .data(entrada.toString())
                .horario("10:30")
                .dataSaidaPrevista(entrada.plusDays(4).toString())
                .status("PENDENTE")
                .build();

        assertEquals(1, AgendamentoRepository.calcularPicoOcupacaoHospedagem(
                List.of(legado),
                LocalDateTime.of(entrada.plusDays(3), LocalTime.NOON),
                LocalDateTime.of(entrada.plusDays(3), LocalTime.of(13, 0))));
        assertEquals(0, AgendamentoRepository.calcularPicoOcupacaoHospedagem(
                List.of(legado),
                LocalDateTime.of(entrada.plusDays(4), LocalTime.NOON),
                LocalDateTime.of(entrada.plusDays(4), LocalTime.of(13, 0))));
    }

    @Test
    void transacaoLeCapacidadeGlobalERejeitaReservaQuandoNaoConfigurada() throws Exception {
        TransacaoHospedagem fixture = new TransacaoHospedagem(false, null);

        assertThrows(AgendamentoRepository.ConflitoReservaException.class,
                () -> fixture.repository.saveIfAvailable(novaHospedagem(), 0));

        verify(fixture.transaction).get(fixture.configClinica);
        verify(fixture.transaction, never()).set(eq(fixture.newBooking), any(AgendamentoModel.class));
    }

    @Test
    void transacaoReservaHospedagemQuandoCapacidadeEstaConfigurada() throws Exception {
        TransacaoHospedagem fixture = new TransacaoHospedagem(true, 3L);

        AgendamentoModel salva = fixture.repository.saveIfAvailable(novaHospedagem(), 0);

        assertEquals("nova", salva.getId());
        verify(fixture.transaction).get(fixture.configClinica);
        verify(fixture.transaction).set(fixture.lock, Map.of("revisao", 1L));
        verify(fixture.transaction).set(fixture.newBooking, salva);
    }

    private AgendamentoModel hospedagem(String id, LocalDateTime inicio, LocalDateTime fim) {
        return AgendamentoModel.builder()
                .id(id)
                .servico("hospedagem")
                .data(inicio.toLocalDate().toString())
                .horario(inicio.toLocalTime().toString())
                .dataSaidaPrevista(fim.toString())
                .tempoHospedagemDias(1)
                .status("PENDENTE")
                .build();
    }

    private AgendamentoModel novaHospedagem() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 9, 0);
        return AgendamentoModel.builder()
                .servico("hospedagem")
                .data(inicio.toLocalDate().toString())
                .horario(inicio.toLocalTime().toString())
                .dataSaidaPrevista(inicio.plusDays(2).toString())
                .tempoHospedagemDias(2)
                .animalId("pet")
                .usuarioId("user")
                .status("PENDENTE")
                .build();
    }

    private static final class TransacaoHospedagem {
        private final Firestore firestore = mock(Firestore.class);
        private final CollectionReference bookings = mock(CollectionReference.class);
        private final CollectionReference locks = mock(CollectionReference.class);
        private final CollectionReference configs = mock(CollectionReference.class);
        private final DocumentReference newBooking = mock(DocumentReference.class);
        private final DocumentReference lock = mock(DocumentReference.class);
        private final DocumentReference configClinica = mock(DocumentReference.class);
        private final DocumentSnapshot lockSnapshot = mock(DocumentSnapshot.class);
        private final DocumentSnapshot configSnapshot = mock(DocumentSnapshot.class);
        private final Query query = mock(Query.class);
        private final QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        private final Transaction transaction = mock(Transaction.class);
        private final AgendamentoRepository repository = new AgendamentoRepository(firestore);

        private TransacaoHospedagem(boolean capacidadeConfigurada, Long capacidade) throws Exception {
            when(firestore.collection("agendamentos")).thenReturn(bookings);
            when(firestore.collection("travasReservas")).thenReturn(locks);
            when(firestore.collection("configuracao")).thenReturn(configs);
            when(bookings.document()).thenReturn(newBooking);
            when(newBooking.getId()).thenReturn("nova");
            when(locks.document("hospedagem")).thenReturn(lock);
            when(configs.document("clinica")).thenReturn(configClinica);
            when(bookings.whereEqualTo("servico", "hospedagem")).thenReturn(query);
            when(querySnapshot.getDocuments()).thenReturn(List.of());
            when(transaction.get(lock)).thenReturn(ApiFutures.immediateFuture(lockSnapshot));
            when(transaction.get(query)).thenReturn(ApiFutures.immediateFuture(querySnapshot));
            when(transaction.get(configClinica)).thenReturn(ApiFutures.immediateFuture(configSnapshot));
            when(lockSnapshot.exists()).thenReturn(false);
            when(configSnapshot.exists()).thenReturn(capacidadeConfigurada);
            when(configSnapshot.getLong("capacidadeHospedagem")).thenReturn(capacidade);
            doAnswer(invocation -> {
                Transaction.Function<?> callback = invocation.getArgument(0);
                try {
                    return ApiFutures.immediateFuture(callback.updateCallback(transaction));
                } catch (Exception exception) {
                    return ApiFutures.immediateFailedFuture(exception);
                }
            }).when(firestore).runTransaction(any(Transaction.Function.class));
        }
    }
}
