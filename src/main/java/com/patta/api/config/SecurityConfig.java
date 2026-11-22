
package com.patta.api.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;
    private final ObjectMapper objectMapper;
    private final String origensPermitidas;

    public SecurityConfig(
            FirebaseTokenFilter firebaseTokenFilter,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins:http://localhost:5500,http://localhost:3000}")
            String origensPermitidas) {
        this.firebaseTokenFilter = firebaseTokenFilter;
        this.objectMapper = objectMapper;
        this.origensPermitidas = origensPermitidas;
    }

    /** Configura a cadeia de segurança da API. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("frame-ancestors 'none'")))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                RespostaErro.escrever(
                                        response, objectMapper, 401,
                                        "Autenticação obrigatória."))
                        .accessDeniedHandler((request, response, exception) ->
                                RespostaErro.escrever(
                                        response, objectMapper, 403,
                                        "Acesso negado.")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET,
                                "/profissional/clinica/status").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/agendamentos/*/status")
                                .hasRole("PROFISSIONAL")
                        .requestMatchers("/profissional/**").hasRole("PROFISSIONAL")
                        .requestMatchers(RotasPublicas.ROTAS_AUTENTICADAS).authenticated()
                        .requestMatchers(RotasPublicas.PADROES).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(
                        firebaseTokenFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Impede o registro duplicado do filtro pelo Spring Boot. */
    @Bean
    public FilterRegistrationBean<FirebaseTokenFilter> registroDesabilitadoDoFiltroDeToken(
            FirebaseTokenFilter filtro) {
        FilterRegistrationBean<FirebaseTokenFilter> registro =
                new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    /** Configura as origens, métodos e cabeçalhos permitidos pelo CORS. */
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

        configuracao.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        configuracao.setAllowedHeaders(List.of(
                "Authorization", "Content-Type"));

        configuracao.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource origem =
                new UrlBasedCorsConfigurationSource();

        origem.registerCorsConfiguration("/**", configuracao);
        return origem;
    }
}
