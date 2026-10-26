package com.patta.api.service;

import com.patta.api.dto.EsqueciSenhaRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;

/**
 * Pede ao Firebase Auth o e-mail de redefinição de senha.
 * O remetente é o próprio Firebase Authentication.
 */
@Slf4j
@Service
public class RedefinicaoSenhaService {

    private static final String URL_ENVIO =
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key={key}";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String webApiKey;

    public RedefinicaoSenhaService(RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper,
                                   @Value("${firebase.web-api-key:}") String webApiKey) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.webApiKey = webApiKey;
    }

    /**
     * Envia o link de redefinição. Se o e-mail não tiver conta, nada é enviado
     * e o método não lança erro, para não revelar quais e-mails estão cadastrados.
     */
    public void enviarLinkRedefinicao(EsqueciSenhaRequestDTO request) {
        if (webApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A propriedade firebase.web-api-key não foi configurada.");
        }

        Map<String, Object> corpo = Map.of(
                "requestType", "PASSWORD_RESET",
                "email", request.email().trim().toLowerCase(Locale.ROOT));

        try {
            restClient.post()
                    .uri(URL_ENVIO, webApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException exception) {
            String codigo = codigoErroFirebase(exception.getResponseBodyAsString());

            switch (codigo) {
                case "EMAIL_NOT_FOUND" ->
                        log.info("Redefinição pedida para e-mail sem conta. Nenhum e-mail enviado.");
                case "TOO_MANY_ATTEMPTS_TRY_LATER" ->
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                "Muitas tentativas. Aguarde alguns minutos e tente de novo.");
                default -> {
                    log.warn("Falha do Firebase ao enviar redefinição. codigo={}", codigo);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Não foi possível enviar o e-mail de redefinição agora.");
                }
            }

        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException) {
                throw exception;
            }
            log.warn("Falha de rede ao enviar redefinição de senha", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível enviar o e-mail de redefinição agora.");
        }
    }

    // Lê o código de erro do JSON do Firebase, por exemplo "EMAIL_NOT_FOUND"
    private String codigoErroFirebase(String corpo) {
        try {
            return objectMapper.readTree(corpo).path("error").path("message").asString("");
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
