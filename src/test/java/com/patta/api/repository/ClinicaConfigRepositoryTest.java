package com.patta.api.repository;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.Transaction;
import com.patta.api.model.ClinicaConfigModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicaConfigRepositoryTest {

    private final Firestore firestore = mock(Firestore.class);
    private final CollectionReference collection = mock(CollectionReference.class);
    private final DocumentReference document = mock(DocumentReference.class);
    private final DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
    private final Transaction transaction = mock(Transaction.class);
    private final ClinicaConfigRepository repository = new ClinicaConfigRepository(firestore);

    @BeforeEach
    void prepararFirestore() throws Exception {
        when(firestore.collection("configuracao")).thenReturn(collection);
        when(collection.document("clinica")).thenReturn(document);
        when(document.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        when(transaction.get(document)).thenReturn(ApiFutures.immediateFuture(snapshot));
        doAnswer(invocation -> {
            Transaction.Function<?> callback = invocation.getArgument(0);
            try {
                return ApiFutures.immediateFuture(callback.updateCallback(transaction));
            } catch (Exception exception) {
                return ApiFutures.immediateFailedFuture(exception);
            }
        }).when(firestore).runTransaction(any(Transaction.Function.class));
    }

    @Test
    void documentoAusenteSignificaClinicaAberta() throws Exception {
        when(snapshot.exists()).thenReturn(false);

        ClinicaConfigModel config = repository.buscar();

        assertEquals(true, config.getAberta());
    }

    @Test
    void ausenciaDoCampoAbertaPreservaCapacidadeEUsaEstadoPadrao() throws Exception {
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.toObject(ClinicaConfigModel.class)).thenReturn(
                ClinicaConfigModel.builder().capacidadeHospedagem(5).build());

        ClinicaConfigModel config = repository.buscar();

        assertEquals(true, config.getAberta());
        assertEquals(5, config.getCapacidadeHospedagem());
    }

    @Test
    void alteracaoTransacionalFazMergeDosCamposDeEstado() throws Exception {
        when(snapshot.exists()).thenReturn(false);
        when(transaction.set(eq(document), any(Map.class), any(SetOptions.class))).thenReturn(transaction);

        ClinicaConfigModel config = repository.alterarEstado("profissional-uid", false);

        assertEquals(false, config.getAberta());
        assertEquals("profissional-uid", config.getAtualizadoPor());
        ArgumentCaptor<Map<String, Object>> campos = ArgumentCaptor.forClass(Map.class);
        verify(transaction).set(eq(document), campos.capture(), any(SetOptions.class));
        assertEquals(false, campos.getValue().get("aberta"));
        assertEquals("profissional-uid", campos.getValue().get("atualizadoPor"));
        org.junit.jupiter.api.Assertions.assertNotNull(campos.getValue().get("atualizadoEm"));
        assertEquals(3, campos.getValue().size());
    }

    @Test
    void transacaoDetectaQuandoEstadoJaFoiSolicitado() {
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getBoolean("aberta")).thenReturn(false);

        assertThrows(ClinicaConfigRepository.EstadoClinicaInalteradoException.class,
                () -> repository.alterarEstado("profissional-uid", false));
    }

    @Test
    void capacidadeEGravadaComMergeSemSobrescreverEstadoAberta() throws Exception {
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.toObject(ClinicaConfigModel.class)).thenReturn(ClinicaConfigModel.builder()
                .aberta(false)
                .capacidadeHospedagem(7)
                .build());
        when(document.set(any(Map.class), any(SetOptions.class)))
                .thenReturn(ApiFutures.immediateFuture(mock(WriteResult.class)));

        ClinicaConfigModel config = repository.atualizarCapacidadeHospedagem(7);

        assertEquals(false, config.getAberta());
        assertEquals(7, config.getCapacidadeHospedagem());
        ArgumentCaptor<Map<String, Object>> campos = ArgumentCaptor.forClass(Map.class);
        verify(document).set(campos.capture(), any(SetOptions.class));
        assertEquals(Map.of("capacidadeHospedagem", 7), campos.getValue());
    }
}
