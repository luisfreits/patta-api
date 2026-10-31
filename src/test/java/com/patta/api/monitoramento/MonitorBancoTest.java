package com.patta.api.monitoramento;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MonitorBancoTest {

    private static final String TOPICO_TESTE = "topico-de-teste";

    private final Firestore firestore = mock(Firestore.class);
    private final CollectionReference colecao = mock(CollectionReference.class);
    private final DocumentReference documento = mock(DocumentReference.class);
    private final ApiFuture<DocumentSnapshot> futuro = mock(ApiFuture.class);
    private final RestClient.Builder restClientBuilder = RestClient.builder();

    private MockRestServiceServer servidor;
    private MonitorBanco monitor;

    @BeforeEach
    void configurar() throws Exception {
        servidor = MockRestServiceServer.bindTo(restClientBuilder).build();
        when(firestore.collection("saude")).thenReturn(colecao);
        when(colecao.document("ping")).thenReturn(documento);
        when(documento.get()).thenReturn(futuro);
        when(futuro.get(5, TimeUnit.SECONDS)).thenReturn(mock(DocumentSnapshot.class));
        monitor = new MonitorBanco(firestore, restClientBuilder, TOPICO_TESTE);
    }

    @Test
    void bancoRespondendoNaoEnviaNotificacao() {
        monitor.verificarSaude();

        servidor.verify();
    }

    @Test
    void duasFalhasSeguidasAindaNaoEnviamNotificacao() throws Exception {
        simularTimeout();

        monitor.verificarSaude();
        monitor.verificarSaude();

        servidor.verify();
    }

    @Test
    void tresFalhasSeguidasEnviamAlertaDeBancoForaDoAr() throws Exception {
        esperarNotificacao("Banco fora do ar", "Firestore indisponível após 3 falhas consecutivas.");
        simularTimeout();

        monitor.verificarSaude();
        monitor.verificarSaude();
        monitor.verificarSaude();

        servidor.verify();
    }

    @Test
    void quartaEQuintaFalhasNaoRepetemOAlerta() throws Exception {
        esperarNotificacao("Banco fora do ar", "Firestore indisponível após 3 falhas consecutivas.");
        simularTimeout();

        monitor.verificarSaude();
        monitor.verificarSaude();
        monitor.verificarSaude();
        monitor.verificarSaude();
        monitor.verificarSaude();

        servidor.verify();
    }

    @Test
    void bancoVoltandoEnviaNotificacaoDeRecuperacao() throws Exception {
        esperarNotificacao("Banco fora do ar", "Firestore indisponível após 3 falhas consecutivas.");
        esperarNotificacao("Banco voltou", "O banco Firestore voltou a responder.");
        simularTimeout();

        monitor.verificarSaude();
        monitor.verificarSaude();
        monitor.verificarSaude();

        doReturn(mock(DocumentSnapshot.class)).when(futuro).get(5, TimeUnit.SECONDS);
        monitor.verificarSaude();

        servidor.verify();
    }

    @Test
    void topicoVazioNaoEnviaNotificacao() throws Exception {
        simularTimeout();
        monitor = new MonitorBanco(firestore, restClientBuilder, "");

        monitor.verificarSaude();
        monitor.verificarSaude();
        monitor.verificarSaude();

        servidor.verify();
    }

    @Test
    void falhaNoEnvioAoNtfyNaoPropagaExcecao() throws Exception {
        servidor.expect(requestTo("https://ntfy.sh/" + TOPICO_TESTE))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());
        simularTimeout();

        monitor.verificarSaude();
        monitor.verificarSaude();
        monitor.verificarSaude();

        assertDoesNotThrow(() -> monitor.verificarSaude());
        servidor.verify();
    }

    private void simularTimeout() throws Exception {
        doThrow(new TimeoutException("tempo esgotado"))
                .when(futuro).get(5, TimeUnit.SECONDS);
    }

    private void esperarNotificacao(String titulo, String mensagem) {
        servidor.expect(requestTo("https://ntfy.sh/" + TOPICO_TESTE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Title", titulo))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(mensagem))
                .andRespond(withSuccess());
    }
}
