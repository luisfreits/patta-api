package com.patta.api.config;

import tools.jackson.databind.ObjectMapper;
import com.patta.api.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Declara as rotas públicas, a política sem sessão e as origens permitidas para o frontend. */
@Configuration // Classe de configuração: cria beans pro Spring
@EnableWebSecurity // Liga o Spring Security e permite personalizar as regras
@RequiredArgsConstructor // Lombok cria o construtor com os campos final
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;
    private final ObjectMapper objectMapper; // Transforma objeto em JSON (respostas de erro)

    // Lê do application.properties as origens (sites) permitidas a chamar a API.
    // Se não existir, usa localhost:5500 e localhost:3000 como padrão
    @Value("${app.cors.allowed-origins:http://localhost:5500,http://localhost:3000}")
    private String allowedOrigins;

    // Define a "cadeia de segurança": as regras que toda requisição passa
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Desliga CSRF (não precisa em API com token, sem cookies)
                // Liga o CORS usando a configuração definida mais embaixo
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // STATELESS: o servidor não guarda sessão. Cada requisição precisa trazer o token
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        // Não autenticado (sem token) tentando rota protegida -> 401 em JSON
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Autenticação obrigatória."))
                        // Autenticado mas sem permissão -> 403 em JSON
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "Acesso negado.")))
                .authorizeHttpRequests(authorize -> authorize
                        // Rotas públicas: qualquer um acessa (login e cadastro)
                        .requestMatchers("/auth/**", "/cadastro/**").permitAll()
                        // Todo o resto exige estar autenticado
                        .anyRequest().authenticated())
                // Coloca o nosso filtro de token ANTES do filtro padrão de login do Spring
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build(); // Monta e devolve a cadeia pronta
    }

    // Configura o CORS: quem pode chamar a API a partir do navegador
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Separa a string por vírgula, tira espaços e ignora itens vazios, virando uma lista
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Headers que o front pode mandar (Authorization é o do token)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Não aceita cookies/credenciais (a autenticação é por header, então não precisa)
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica essa configuração em todas as rotas
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Monta e envia a resposta de erro de segurança em JSON (usada pelo 401 e 403 acima)
    private void writeSecurityError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiErrorDTO.builder()
                .status(status).mensagem(message).timestamp(Instant.now()).build());
    }
}