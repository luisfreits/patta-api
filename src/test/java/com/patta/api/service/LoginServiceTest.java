package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.patta.api.dto.LoginRequestDTO;
import com.patta.api.dto.LoginResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LoginServiceTest {

    @Test
    void retornaTokensVerificacaoEPerfilEmUmaRespostaDeLogin() throws Exception {
        String apiKey = "test-api-key";
        String uid = "user-uid";
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
                        + apiKey))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "email": "ana@example.test",
                          "password": "senha-segura",
                          "returnSecureToken": true
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "localId": "user-uid",
                          "idToken": "firebase-id-token",
                          "refreshToken": "refresh-token",
                          "expiresIn": "3600"
                        }
                        """, MediaType.APPLICATION_JSON));

        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        UserRecord usuario = mock(UserRecord.class);
        when(firebaseAuth.getUser(uid)).thenReturn(usuario);
        when(usuario.isEmailVerified()).thenReturn(true);
        PapelUsuarioService papelUsuarioService = new PapelUsuarioService(uid);
        LoginService service = new LoginService(
                restClientBuilder, new ObjectMapper(), apiKey, firebaseAuth, papelUsuarioService);

        LoginResponseDTO resposta = service.login(
                new LoginRequestDTO("ana@example.test", "senha-segura"));

        assertEquals("firebase-id-token", resposta.idToken());
        assertEquals("refresh-token", resposta.refreshToken());
        assertEquals(3600L, resposta.expiresIn());
        assertEquals(true, resposta.emailVerified());
        assertEquals("ADMIN", resposta.role());
        verify(firebaseAuth).getUser(uid);
        server.verify();
    }
}
