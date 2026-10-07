package com.patta.api.service;

// Jackson 3 é a implementação gerenciada pelo Spring Boot 4.
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.patta.api.dto.LoginRequestDTO;
import com.patta.api.dto.LoginResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Map;

/** Troca e-mail e senha por tokens Firebase sem criar sessão própria no backend. */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${firebase.web-api-key:}")
    private String webApiKey;

    /** O token retornado é posteriormente validado pelo FirebaseTokenFilter em cada rota protegida. */
    public LoginResponseDTO login(LoginRequestDTO request) {
        if (webApiKey == null || webApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A propriedade firebase.web-api-key não foi configurada.");
        }

        String endpoint = UriComponentsBuilder
                .fromUriString("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword")
                .queryParam("key", webApiKey)
                .build()
                .encode()
                .toUriString();
        Map<String, Object> credentials = Map.of(
                "email", request.getEmail().trim().toLowerCase(Locale.ROOT),
                "password", request.getSenha(),
                "returnSecureToken", true);

        try {
            String json = restClientBuilder.build().post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credentials)
                    .retrieve()
                    .body(String.class);
            JsonNode tokenResponse = objectMapper.readTree(json);
                // Jackson 3 usa asString() como accessor atual para valores textuais do JSON.
            return LoginResponseDTO.builder()
                    .idToken(tokenResponse.path("idToken").asString())
                    .refreshToken(tokenResponse.path("refreshToken").asString())
                    .expiresIn(tokenResponse.path("expiresIn").asString())
                    .build();
        } catch (RestClientResponseException exception) {
            String firebaseCode = firebaseErrorCode(exception.getResponseBodyAsString());
            if (isInvalidCredential(firebaseCode)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O serviço de autenticação do Firebase não está disponível.");
        } catch (Exception exception) {
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível concluir o login no Firebase.");
        }
    }

    private String firebaseErrorCode(String body) {
        try {
            return objectMapper.readTree(body).path("error").path("message").asString("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isInvalidCredential(String errorCode) {
        return switch (errorCode) {
            case "INVALID_LOGIN_CREDENTIALS", "EMAIL_NOT_FOUND", "INVALID_PASSWORD",
                    "USER_DISABLED", "INVALID_EMAIL" -> true;
            default -> false;
        };
    }
}