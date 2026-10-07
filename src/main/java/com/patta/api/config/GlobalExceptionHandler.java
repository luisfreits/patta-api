package com.patta.api.config;

import com.patta.api.dto.ApiErrorDTO;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

/** Converte exceções lançadas pelas camadas internas no contrato JSON padrão da API. */
@RestControllerAdvice // Vale para todos os controllers: captura exceções e devolve JSON
public class GlobalExceptionHandler {

    // Erro de validação do @Valid (ex.: campo obrigatório vazio no AnimalRequestDTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException exception) {
        // Pega as mensagens de cada campo inválido, tira repetidas (distinct) e junta num texto só
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).distinct().collect(Collectors.joining(" "));
        return response(HttpStatus.BAD_REQUEST, message); // 400
    }

    // Validação de parâmetros fora do corpo (ex.: @PathVariable ou @RequestParam com restrição)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleConstraint(ConstraintViolationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage()); // 400
    }

    // Exceção que o próprio código lança de propósito (ex.: 404 animal não encontrado no service)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorDTO> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        // Spring fornece o texto padrão pelo nome getReasonPhrase; getReason não existe em HttpStatus.
        // Se a exceção não trouxe mensagem, usa o texto padrão do status (ex.: "Not Found")
        return response(status, exception.getReason() == null ? status.getReasonPhrase() : exception.getReason());
    }

    // Sem permissão para executar o método -> 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied() {
        return response(HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    // Rede de segurança: qualquer outro erro não previsto vira 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleUnexpected(Exception exception) {
        // Mensagem genérica de propósito: não expõe detalhes internos pro cliente
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.");
    }

    // Método auxiliar: monta a resposta no formato padrão (status, mensagem, horário)
    private ResponseEntity<ApiErrorDTO> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiErrorDTO.builder()
                .status(status.value()).mensagem(message).timestamp(Instant.now()).build());
    }
}