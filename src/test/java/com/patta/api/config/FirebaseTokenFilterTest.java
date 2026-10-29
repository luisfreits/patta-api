package com.patta.api.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.patta.api.service.PapelUsuarioService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebaseTokenFilterTest {

    @Test
    void tokenInvalidoERejeitadoAntesDeProsseguirNaCadeia() throws Exception {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        when(firebaseAuth.verifyIdToken("invalid-token"))
                .thenThrow(new IllegalArgumentException("invalid"));
        FirebaseTokenFilter filter = new FirebaseTokenFilter(
                firebaseAuth, new ObjectMapper(), new PapelUsuarioService(""));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/animais");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
        verify(firebaseAuth, never()).getUser(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void tokenAusenteNaoEValidadoPeloFirebaseFilter() throws Exception {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseTokenFilter filter = new FirebaseTokenFilter(
                firebaseAuth, new ObjectMapper(), new PapelUsuarioService(""));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/animais");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(firebaseAuth, never()).verifyIdToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void tokenAutenticadoRecebePapelDefinidoPeloUidNoServidor() throws Exception {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        UserRecord usuario = mock(UserRecord.class);
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(token);
        when(token.getUid()).thenReturn("admin-uid");
        when(firebaseAuth.getUser("admin-uid")).thenReturn(usuario);
        when(usuario.isDisabled()).thenReturn(false);
        when(usuario.isEmailVerified()).thenReturn(true);
        FirebaseTokenFilter filter = new FirebaseTokenFilter(
                firebaseAuth, new ObjectMapper(), new PapelUsuarioService("admin-uid"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        try {
            filter.doFilter(request, response, chain);

            assertEquals("admin-uid", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            assertEquals("ROLE_ADMIN", SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().iterator().next().getAuthority());
            verify(chain).doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
