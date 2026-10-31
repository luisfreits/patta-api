package com.patta.api.monitoramento;

import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MonitorBanco {

    private static final int FALHAS_PARA_ALERTAR = 3;
    private static final String URL_NTFY = "https://ntfy.sh/{topico}";

    private final Firestore firestore;
    private final RestClient restClient;
    private final String topico;

    private int falhasSeguidas;
    private boolean alertaEnviado;

    public MonitorBanco(Firestore firestore,
                        RestClient.Builder restClientBuilder,
                        @Value("${NTFY_TOPICO:}") String topico) {
        this.firestore = firestore;
        this.restClient = restClientBuilder.build();
        this.topico = topico;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    synchronized void verificarSaude() {
        try {
            // A leitura serve apenas para testar a conexão; o documento pode não existir.
            firestore.collection("saude").document("ping").get().get(5, TimeUnit.SECONDS);

            if (alertaEnviado) {
                notificar("Banco voltou", "O banco Firestore voltou a responder.");
                alertaEnviado = false;
            }
            falhasSeguidas = 0;
            log.info("Firestore respondeu ao teste de saúde. status=OK");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            registrarFalha(exception);
        } catch (Exception exception) {
            registrarFalha(exception);
        }
    }

    private void registrarFalha(Exception exception) {
        // Limitar o contador evita crescimento sem necessidade durante uma queda longa.
        falhasSeguidas = Math.min(falhasSeguidas + 1, FALHAS_PARA_ALERTAR);
        log.warn("Firestore indisponível. status=FALHA; falhasSeguidas={}; erro={}",
                falhasSeguidas, exception.getMessage());

        if (!alertaEnviado && falhasSeguidas >= FALHAS_PARA_ALERTAR) {
            // A tentativa também encerra o alerta da queda para não repetir a cada execução.
            alertaEnviado = true;
            notificar("Banco fora do ar", "Firestore indisponível após 3 falhas consecutivas.");
        }
    }

    private void notificar(String titulo, String mensagem) {
        if (topico.isBlank()) {
            log.warn("Notificação não enviada: NTFY_TOPICO não configurado. título={}", titulo);
            return;
        }

        try {
            restClient.post()
                    .uri(URL_NTFY, topico)
                    .header("Title", titulo)
                    .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                    .body(mensagem)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notificação ntfy enviada. status=OK; título={}", titulo);
        } catch (Exception exception) {
            log.error("Falha ao enviar notificação ntfy. status=FALHA; erro={}", exception.getMessage());
        }
    }
}
