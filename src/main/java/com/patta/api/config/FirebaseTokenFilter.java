package com.patta.api.config;

// Spring Boot 4 usa Jackson 3 para serializar as respostas JSON.
import tools.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.patta.api.dto.ApiErrorDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Valida cada bearer token uma vez e publica somente o uid confirmado no contexto de segurança. */
@Component // O Spring cria esse filtro e registra como bean
@RequiredArgsConstructor // Lombok cria o construtor com os campos final
public class FirebaseTokenFilter extends OncePerRequestFilter { // Garante que o filtro roda 1 vez por requisição

    private final FirebaseAuth firebaseAuth; // Bean do FirebaseConfig, valida o token
    private final ObjectMapper objectMapper; // Transforma objeto Java em JSON (pra resposta de erro)

    // Decide em quais rotas o filtro NÃO roda
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Rotas públicas não dependem de um token nem são bloqueadas por token inválido enviado por engano.
        String path = request.getServletPath();
        // true = pula o filtro. Aqui: /auth e /cadastro (login e cadastro não têm token ainda)
        return path.startsWith("/auth/") || path.equals("/auth")
                || path.startsWith("/cadastro/") || path.equals("/cadastro");
    }

    // O trabalho principal do filtro, roda em toda requisição que não foi pulada
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Lê o header Authorization (deveria vir "Bearer <token>")
        String authorization = request.getHeader("Authorization");

        // Sem header: só segue em frente SEM autenticar.
        // Se a rota for protegida, o Spring Security barra depois (401/403)
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Header existe mas está mal formado (não começa com "Bearer " ou não tem token): 401 direto
        if (!authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            writeUnauthorized(response);
            return;
        }

        String uid;
        try {
            // Firebase verifica assinatura, expiração e projeto; o UID nunca é aceito do cliente.
            // substring(7) tira o "Bearer " e deixa só o token. getUid() pega o dono do token
            uid = firebaseAuth.verifyIdToken(authorization.substring(7)).getUid();
        } catch (FirebaseAuthException | IllegalArgumentException exception) {
            // Token falso, expirado ou inválido: limpa o contexto e devolve 401
            SecurityContextHolder.clearContext();
            writeUnauthorized(response);
            return;
        }

        // Cria o objeto de "usuário autenticado": principal = uid, sem senha (null), papel ROLE_USER
        var authentication = new UsernamePasswordAuthenticationToken(
                uid, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        // Guarda no contexto de segurança. É daqui que o @AuthenticationPrincipal lê o uid
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Passa a requisição adiante (próximo filtro e depois o controller)
        filterChain.doFilter(request, response);
    }

    // Monta e envia a resposta 401 em JSON
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Status 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // Resposta é JSON
        response.setCharacterEncoding("UTF-8"); // Pra acentos saírem certos
        // Escreve no corpo da resposta um objeto de erro padronizado
        objectMapper.writeValue(response.getWriter(), ApiErrorDTO.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .mensagem("Token ausente ou inválido.")
                .timestamp(Instant.now())
                .build());
    }
}