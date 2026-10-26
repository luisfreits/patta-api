package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.Mockito.mock;

class FirebaseEmailVerificationServiceTest {

    private static final String API_KEY = "test-api-key";
    private static final String CONTINUE_URL = "https://app.example.test/email-verified";

    @Test
    void enviaLinkPeloEndpointOficialDoFirebaseEmPortugues() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
                        + API_KEY))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "email": "ana@example.test",
                          "password": "senha-segura",
                          "returnSecureToken": true
                        }
                        """))
                .andRespond(withSuccess("{\"idToken\":\"firebase-id-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key="
                        + API_KEY))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Firebase-Locale", "pt-BR"))
                .andExpect(content().json("""
                        {
                          "requestType": "VERIFY_EMAIL",
                          "idToken": "firebase-id-token",
                          "continueUrl": "https://app.example.test/email-verified"
                        }
                        """))
                .andRespond(withSuccess());

        FirebaseEmailVerificationService service = service(restClientBuilder, CONTINUE_URL);

        service.enviarParaNovaConta("ana@example.test", "senha-segura");

        server.verify();
    }

    @Test
    void traduzFalhaNoEnvioEmErroDeServicoDisponivel() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
                        + API_KEY))
                .andRespond(withSuccess("{\"idToken\":\"firebase-id-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key="
                        + API_KEY))
                .andRespond(withBadRequest().body("{\"error\":{\"message\":\"INVALID_ID_TOKEN\"}}"));

        FirebaseEmailVerificationService service = service(restClientBuilder, "");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.enviarParaNovaConta("ana@example.test", "senha-segura"));

        assertEquals(503, exception.getStatusCode().value());
        server.verify();
    }

    private FirebaseEmailVerificationService service(RestClient.Builder restClientBuilder, String continueUrl) {
        return new FirebaseEmailVerificationService(
                mock(FirebaseAuth.class),
                restClientBuilder,
                new ObjectMapper(),
                API_KEY,
                continueUrl);
    }
}
