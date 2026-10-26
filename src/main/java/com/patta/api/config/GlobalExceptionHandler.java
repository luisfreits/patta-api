package com.patta.api.config;

import com.patta.api.dto.ApiErrorDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converte as exceções da aplicação no formato JSON padrão (ApiErrorDTO).
 * Erros inesperados viram 500 com mensagem genérica; o detalhe vai para o log.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Campos inválidos no corpo (@Valid). Inclui erros de classe, como a confirmação de senha. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> validacaoDoCorpo(MethodArgumentNotValidException exception) {
        // getAllErrors inclui erros de campo e erros globais (como o @AssertTrue do CadastroRequestDTO)
        String mensagem = exception.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    /** Restrições em parâmetros de método. Usa só a mensagem, sem o nome interno do parâmetro. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDTO> validacaoDeParametro(ConstraintViolationException exception) {
        String mensagem = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    /** JSON malformado ou tipos errados no corpo. Sem este handler, cairia em 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> corpoIlegivel(HttpMessageNotReadableException exception) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formado.");
    }

    /** Erros lançados de propósito pelos services (ex.: 404, 409, 502). Mantêm o status e a mensagem. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorDTO> erroExplicito(ResponseStatusException exception) {
        HttpStatusCode codigo = exception.getStatusCode();
        String mensagem = exception.getReason() != null
                ? exception.getReason()
                : HttpStatus.valueOf(codigo.value()).getReasonPhrase();
        return ResponseEntity.status(codigo).body(montarErro(codigo.value(), mensagem));
    }

    /** Rota existe, mas o verbo HTTP não é aceito. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorDTO> metodoNaoPermitido() {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido para esta rota.");
    }

    /** Nenhuma rota corresponde ao caminho. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorDTO> rotaNaoEncontrada() {
        return resposta(HttpStatus.NOT_FOUND, "Rota não encontrada.");
    }

    /** Usuário autenticado sem permissão para a ação. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> acessoNegado() {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    /** Rede de segurança: qualquer outro erro vira 500 com mensagem genérica. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> erroInesperado(Exception exception) {
        log.error("Erro não tratado", exception); // O detalhe fica no log, não na resposta
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.");
    }

    private ResponseEntity<ApiErrorDTO> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(montarErro(status.value(), mensagem));
    }

    private ApiErrorDTO montarErro(int status, String mensagem) {
        return ApiErrorDTO.builder()
                .status(status)
                .mensagem(mensagem)
                .timestamp(Instant.now())
                .build();
    }
}
