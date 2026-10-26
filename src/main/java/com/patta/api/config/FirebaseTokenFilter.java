package com.patta.api.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.UserRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Valida o token Bearer do Firebase em cada requisição protegida e grava o uid
 * confirmado no contexto de segurança. O uid nunca é aceito vindo do cliente.
 */
@Component
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";
    private static final String MENSAGEM_NAO_AUTORIZADO = "Token ausente ou inválido.";
    private static final List<SimpleGrantedAuthority> PAPEIS = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final FirebaseAuth firebaseAuth;
    private final ObjectMapper objectMapper;

    /** Rotas públicas passam sem análise de token; status e reenvio exigem autenticação. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return RotasPublicas.isPublica(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Sem cabeçalho: segue sem autenticar. Se a rota for protegida, o Spring Security responde 401
        if (cabecalho == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Cabeçalho presente, mas fora do formato "Bearer <token>": rejeita já aqui
        if (!cabecalho.startsWith(PREFIXO_BEARER) || cabecalho.length() <= PREFIXO_BEARER.length()) {
            RespostaErro.escrever(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, MENSAGEM_NAO_AUTORIZADO);
            return;
        }

        String uid;
        try {
            // verifyIdToken confere assinatura, expiração e projeto.
            // Para também detectar tokens revogados (ex.: conta excluída), use verifyIdToken(token, true).
            // Isso faz uma chamada extra ao Firebase em cada requisição.
            uid = firebaseAuth.verifyIdToken(cabecalho.substring(PREFIXO_BEARER.length())).getUid();
        } catch (FirebaseAuthException | IllegalArgumentException exception) {
            // Token falso, expirado ou vazio
            RespostaErro.escrever(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, MENSAGEM_NAO_AUTORIZADO);
            return;
        }

        UserRecord usuario;
        try {
            // Consulta Admin Auth em cada chamada para não confiar em claims de verificação desatualizadas.
            usuario = firebaseAuth.getUser(uid);
        } catch (FirebaseAuthException exception) {
            if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                RespostaErro.escrever(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                        MENSAGEM_NAO_AUTORIZADO);
                return;
            }
            RespostaErro.escrever(response, objectMapper, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Não foi possível validar a conta no Firebase.");
            return;
        }

        if (usuario.isDisabled()) {
            RespostaErro.escrever(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                    MENSAGEM_NAO_AUTORIZADO);
            return;
        }

        if (!usuario.isEmailVerified() && !RotasPublicas.permiteContaNaoVerificada(request.getServletPath())) {
            RespostaErro.escrever(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                    "Confirme seu endereço de e-mail para acessar este recurso.");
            return;
        }

        // Principal = uid, sem credencial (null) e com o papel ROLE_USER.
        // É daqui que o @AuthenticationPrincipal lê o uid nos controllers.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(uid, null, PAPEIS));
        filterChain.doFilter(request, response);
    }
}
