package com.patta.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

/**
 * Regras de segurança da API: rotas públicas, sem sessão no servidor,
 * respostas de erro em JSON e origens permitidas para o frontend.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;
    private final ObjectMapper objectMapper;
    private final String origensPermitidas;

    public SecurityConfig(FirebaseTokenFilter firebaseTokenFilter,
                          ObjectMapper objectMapper,
                          @Value("${app.cors.allowed-origins:http://localhost:5500,http://localhost:3000}")
                          String origensPermitidas) {
        this.firebaseTokenFilter = firebaseTokenFilter;
        this.objectMapper = objectMapper;
        this.origensPermitidas = origensPermitidas;
    }

    /** Cadeia de filtros que toda requisição atravessa. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF só se aplica a sessões com cookie. Esta API usa token no header, então fica desligado
                .csrf(AbstractHttpConfigurer::disable)
                // Usa o bean CorsConfigurationSource definido abaixo
                .cors(Customizer.withDefaults())
                // Sem sessão: cada requisição precisa trazer o próprio token
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(erros -> erros
                        // Sem autenticação em rota protegida -> 401 em JSON
                        .authenticationEntryPoint((request, response, exception) ->
                                RespostaErro.escrever(response, objectMapper, 401, "Autenticação obrigatória."))
                        // Autenticado sem permissão -> 403 em JSON
                        .accessDeniedHandler((request, response, exception) ->
                                RespostaErro.escrever(response, objectMapper, 403, "Acesso negado.")))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(RotasPublicas.ROTAS_AUTENTICADAS).authenticated()
                        .requestMatchers(RotasPublicas.PADROES).permitAll()
                        .anyRequest().authenticated())
                // O filtro de token roda antes do filtro padrão de login
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Impede que o Spring Boot registre o FirebaseTokenFilter também como filtro de servlet.
     * Sem isso, ele seria executado duas vezes, e a segunda execução pode perder a autenticação.
     * Assim ele roda apenas dentro da cadeia de segurança.
     */
    @Bean
    public FilterRegistrationBean<FirebaseTokenFilter> registroDesabilitadoDoFiltroDeToken(
            FirebaseTokenFilter filtro) {
        FilterRegistrationBean<FirebaseTokenFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    /** Origens do navegador autorizadas a chamar a API, lidas de app.cors.allowed-origins. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList());
        configuracao.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracao.setAllowCredentials(false); // Autenticação é por header, não por cookie

        UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
        origem.registerCorsConfiguration("/**", configuracao);
        return origem;
    }
}
