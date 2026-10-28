package com.patta.api.config;

import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
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
        FirebaseTokenFilter filter = new FirebaseTokenFilter(firebaseAuth, new ObjectMapper());
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
        FirebaseTokenFilter filter = new FirebaseTokenFilter(firebaseAuth, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/animais");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(firebaseAuth, never()).verifyIdToken(org.mockito.ArgumentMatchers.anyString());
    }
}
