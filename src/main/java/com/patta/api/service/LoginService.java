package com.patta.api.service;

import com.patta.api.dto.LoginRequestDTO;
import com.patta.api.dto.LoginResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Autentica no Firebase Auth via REST e devolve os tokens ao cliente.
 * Não cria sessão no backend: a validação acontece depois, no filtro de cada rota protegida.
 */
@Service
public class LoginService {

    // Endpoint REST do Firebase Auth. {key} é substituído pela chave Web API, com encoding correto
    private static final String URL_LOGIN =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={key}";

    // Códigos de erro do Firebase que significam "credenciais erradas" (devolvem 401 ao cliente)
    private static final Set<String> CODIGOS_CREDENCIAL_INVALIDA = Set.of(
            "INVALID_LOGIN_CREDENTIALS", "EMAIL_NOT_FOUND", "INVALID_PASSWORD",
            "USER_DISABLED", "INVALID_EMAIL");

    private final RestClient restClient;   // Criado uma vez e reutilizado em todas as chamadas
    private final ObjectMapper objectMapper;
    private final String webApiKey;

    // Construtor explícito para ler a chave do application.properties com @Value
    // Sem chave configurada, a string fica vazia e o login responde 503 (não derruba a aplicação na inicialização)
    public LoginService(RestClient.Builder restClientBuilder,
                        ObjectMapper objectMapper,
                        @Value("${firebase.web-api-key:}") String webApiKey) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.webApiKey = webApiKey;
    }

    /**
     * Troca e-mail e senha pelos tokens do Firebase.
     * Erros do Firebase viram 401 (credencial errada) ou 502 (falha do serviço externo).
     */
    public LoginResponseDTO login(LoginRequestDTO request) {
        if (webApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A propriedade firebase.web-api-key não foi configurada.");
        }

        // O e-mail é normalizado igual ao cadastro, para que "Ana@x.com" e "ana@x.com" sejam o mesmo usuário
        Map<String, Object> credenciais = Map.of(
                "email", request.email().trim().toLowerCase(Locale.ROOT),
                "password", request.senha(),
                "returnSecureToken", true);

        try {
            String json = restClient.post()
                    .uri(URL_LOGIN, webApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credenciais)
                    .retrieve()
                    .body(String.class);

            JsonNode resposta = objectMapper.readTree(json);
            return LoginResponseDTO.builder()
                    .idToken(resposta.path("idToken").asString())
                    .refreshToken(resposta.path("refreshToken").asString())
                    // O Firebase envia expiresIn como texto ("3600"); o DTO espera Long
                    .expiresIn(Long.parseLong(resposta.path("expiresIn").asString("0")))
                    .build();

        } catch (RestClientResponseException exception) {
            // O Firebase respondeu com erro HTTP (4xx/5xx): o corpo traz o código específico
            if (CODIGOS_CREDENCIAL_INVALIDA.contains(codigoErroFirebase(exception.getResponseBodyAsString()))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O serviço de autenticação do Firebase não está disponível.");

        } catch (RuntimeException exception) {
            // Falha de rede, JSON malformado ou número inválido em expiresIn
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível concluir o login no Firebase.");
        }
    }

    // Extrai o código de erro (ex.: "INVALID_PASSWORD") do corpo JSON da resposta do Firebase
    private String codigoErroFirebase(String corpo) {
        try {
            return objectMapper.readTree(corpo).path("error").path("message").asString("");
        } catch (RuntimeException ignored) {
            // Corpo que não é JSON: tratado como erro genérico
            return "";
        }
    }
}
