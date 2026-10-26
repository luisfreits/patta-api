package com.patta.api.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends Firebase Authentication email-verification actions through its official REST API.
 */
@Service
@Slf4j
public class FirebaseEmailVerificationService {

    private static final String URL_LOGIN =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={key}";
    private static final String URL_SEND_OOB_CODE =
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key={key}";
    private static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);

    private final FirebaseAuth firebaseAuth;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String webApiKey;
    private final String continueUrl;
    private final Map<String, Instant> lastResendByUid = new ConcurrentHashMap<>();

    public FirebaseEmailVerificationService(
            FirebaseAuth firebaseAuth,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${firebase.web-api-key:}") String webApiKey,
            @Value("${firebase.email-action-continue-url:}") String continueUrl) {
        this.firebaseAuth = firebaseAuth;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.webApiKey = webApiKey;
        this.continueUrl = continueUrl;
    }

    public void enviarParaNovaConta(String email, String senha) {
        String idToken = obterIdToken(email, senha);
        enviarCodigoAcao(idToken);
    }

    public void reenviar(String uid, String idToken) {
        UserRecord usuario = buscarUsuario(uid);
        if (usuario.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já foi verificado.");
        }

        aplicarLimiteDeReenvio(uid);
        enviarCodigoAcao(idToken);
    }

    public boolean emailVerificado(String uid) {
        return buscarUsuario(uid).isEmailVerified();
    }

    private String obterIdToken(String email, String senha) {
        exigirWebApiKey();
        Map<String, Object> credenciais = Map.of(
                "email", email,
                "password", senha,
                "returnSecureToken", true);
        try {
            String resposta = restClient.post()
                    .uri(URL_LOGIN, webApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credenciais)
                    .retrieve()
                    .body(String.class);
            JsonNode idToken = objectMapper.readTree(resposta).path("idToken");
            if (idToken.isMissingNode() || idToken.asString().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "O Firebase não retornou uma sessão válida para enviar a verificação.");
            }
            return idToken.asString();
        } catch (RestClientResponseException exception) {
            log.warn("Falha na autenticação REST do Firebase para envio da verificação: status={}",
                    exception.getStatusCode());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível iniciar o envio do e-mail de verificação.");
        } catch (RestClientException exception) {
            log.warn("Falha de conexão com o Firebase ao iniciar a verificação.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível conectar ao serviço de autenticação.");
        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException statusException) {
                throw statusException;
            }
            log.warn("Resposta inválida do Firebase ao iniciar a verificação.");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível iniciar o envio do e-mail de verificação.");
        }
    }

    private void enviarCodigoAcao(String idToken) {
        exigirWebApiKey();
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("requestType", "VERIFY_EMAIL");
        pedido.put("idToken", idToken);
        if (!continueUrl.isBlank()) {
            pedido.put("continueUrl", continueUrl);
        }

        try {
            restClient.post()
                    .uri(URL_SEND_OOB_CODE, webApiKey)
                    .header("X-Firebase-Locale", "pt-BR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(pedido)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.warn("Firebase recusou o envio do e-mail de verificação: status={}",
                    exception.getStatusCode());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail de verificação pelo Firebase.");
        } catch (RestClientException exception) {
            log.warn("Falha de conexão com o Firebase ao enviar a verificação.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível conectar ao serviço de autenticação.");
        }
    }

    private UserRecord buscarUsuario(String uid) {
        try {
            return firebaseAuth.getUser(uid);
        } catch (FirebaseAuthException exception) {
            if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A conta autenticada não existe.");
            }
            log.warn("Falha ao consultar conta no Firebase Authentication: authErrorCode={}",
                    exception.getAuthErrorCode());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível validar o estado da conta no Firebase.");
        }
    }

    private void aplicarLimiteDeReenvio(String uid) {
        Instant agora = Instant.now();
        synchronized (lastResendByUid) {
            lastResendByUid.entrySet().removeIf(entry ->
                    entry.getValue().plus(RESEND_INTERVAL).isBefore(agora));
            Instant ultimoEnvio = lastResendByUid.get(uid);
            if (ultimoEnvio != null && ultimoEnvio.plus(RESEND_INTERVAL).isAfter(agora)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Aguarde antes de solicitar outro e-mail de verificação.");
            }
            lastResendByUid.put(uid, agora);
        }
    }

    private void exigirWebApiKey() {
        if (webApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A propriedade firebase.web-api-key não foi configurada.");
        }
    }
}
