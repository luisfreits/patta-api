package com.patta.api.monitoramento;

import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

/**
 * Verifica o Firestore a cada minuto e avisa no celular via ntfy
 * quando o banco fica inacessível por 3 verificações seguidas.
 */
@Slf4j
@Component
public class MonitorBanco {

    private static final int FALHAS_PARA_ALERTAR = 3;
    private static final String URL_NTFY = "https://ntfy.sh/{topico}";

    private final Firestore firestore;
    private final RestClient.Builder restClientBuilder;
    private final String topico;

    private int falhasSeguidas = 0;

    // Só fica true quando o alerta de queda chegou ao celular.
    // Assim, se o envio falhar, o monitor tenta de novo no minuto seguinte.
    private boolean alertaEnviado = false;

    public MonitorBanco(Firestore firestore,
                        RestClient.Builder restClientBuilder,
                        @Value("${NTFY_TOPICO:}") String topico) {
        this.firestore = firestore;
        this.restClientBuilder = restClientBuilder;
        this.topico = topico;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void verificarSaude() {
        if (bancoResponde()) {
            if (alertaEnviado && notificar("Banco voltou", "O banco Firestore voltou a responder.")) {
                alertaEnviado = false;
            }
            falhasSeguidas = 0;
            return;
        }

        falhasSeguidas++;
        log.warn("Firestore indisponível. status=FALHA; falhasSeguidas={}", falhasSeguidas);

        if (falhasSeguidas >= FALHAS_PARA_ALERTAR && !alertaEnviado) {
            alertaEnviado = notificar("Banco fora do ar",
                    "Firestore indisponível após " + FALHAS_PARA_ALERTAR + " falhas consecutivas.");
        }
    }

    // Leitura de um documento de teste. O documento não precisa existir:
    // basta o Firestore responder dentro de 5 segundos.
    private boolean bancoResponde() {
        try {
            firestore.collection("saude").document("ping").get().get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    // Devolve true só se a notificação foi aceita pelo ntfy
    private boolean notificar(String titulo, String mensagem) {
        if (topico == null || topico.isBlank()) {
            log.warn("NTFY_TOPICO não configurado. Alerta não enviado: {}", titulo);
            return false;
        }
        try {
            restClientBuilder.build().post()
                    .uri(URL_NTFY, topico)
                    .header("Title", titulo)
                    .body(mensagem)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException exception) {
            log.error("Falha ao enviar notificação ntfy. status=FALHA; erro={}", exception.getMessage());
            return false;
        }
    }
}