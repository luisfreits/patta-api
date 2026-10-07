package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.UserRecord;
import com.patta.api.dto.CadastroRequestDTO;
import com.patta.api.model.UserModel;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/** Coordena a solicitação e a confirmação do cadastro sem gravar senha no Firestore. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CadastroService {

    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final long CODE_LIFETIME_SECONDS = 15 * 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FirebaseAuth firebaseAuth;

    // O mapa é temporário e local ao processo; em produção, várias instâncias pedem um armazenamento compartilhado.
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    /** Confere duplicidade primeiro, e só então guarda os dados temporários e envia o código. */
    public void solicitarCadastro(CadastroRequestDTO request) {
        cleanupExpiredRegistrations(null);
        String email = normalizeEmail(request.getEmail());
        if (!request.getSenha().equals(request.getConfirmaSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As senhas não coincidem.");
        }
        if (pendingRegistrations.containsKey(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um cadastro pendente para este e-mail.");
        }
        ensureEmailDoesNotExist(email);

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        CadastroRequestDTO normalizedRequest = new CadastroRequestDTO();
        normalizedRequest.setNome(request.getNome().trim());
        normalizedRequest.setEmail(email);
        normalizedRequest.setTelefone(request.getTelefone().trim());
        normalizedRequest.setSenha(request.getSenha());
        normalizedRequest.setConfirmaSenha(request.getConfirmaSenha());
        pendingRegistrations.put(email, new PendingRegistration(
                normalizedRequest, code, Instant.now().plusSeconds(CODE_LIFETIME_SECONDS), 0));

        try {
            emailService.enviarCodigoVerificacao(email, code);
        } catch (RuntimeException exception) {
            log.error("Falha ao enviar e-mail de confirmação", exception);
            pendingRegistrations.remove(email);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail de confirmação.");
        }
    }

    /** Valida código/expiração e cria a identidade; o mesmo uid identifica o documento no Firestore. */
    public UserModel confirmarCadastro(String rawEmail, String code)
            throws ExecutionException, InterruptedException {
        String email = normalizeEmail(rawEmail);
        cleanupExpiredRegistrations(email);
        PendingRegistration pending = pendingRegistrations.get(email);
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Solicitação de cadastro não encontrada ou expirada.");
        }

        synchronized (pending) {
            if (pending.expiration().isBefore(Instant.now())) {
                pendingRegistrations.remove(email, pending);
                throw new ResponseStatusException(HttpStatus.GONE, "O código expirou. Solicite um novo cadastro.");
            }
            if (!pending.code().equals(code)) {
                int attempts = pending.incrementAttempts();
                if (attempts >= MAX_CODE_ATTEMPTS) {
                    pendingRegistrations.remove(email, pending);
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Limite de tentativas atingido. Solicite um novo código.");
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de confirmação inválido.");
            }

            UserRecord created;
            try {
                UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                        .setEmail(email)
                    .setEmailVerified(true)
                        .setPassword(pending.request().getSenha());
                created = firebaseAuth.createUser(createRequest);
            } catch (FirebaseAuthException exception) {
                // Comparamos o enum tipado: getErrorCode() retorna texto e não corresponde a AuthErrorCode.
                if (exception.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                    pendingRegistrations.remove(email, pending);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível criar a conta no Firebase Authentication.");
            }

            UserModel user = UserModel.builder()
                    .id(created.getUid())
                    .nome(pending.request().getNome())
                    .email(email)
                    .telefone(pending.request().getTelefone())
                    .build();
            try {
                // Este UID também é o ID do documento users/{uid}; senha não é copiada para o model.
                UserModel saved = userRepository.save(user);
                pendingRegistrations.remove(email, pending);
                return saved;
            } catch (ExecutionException | InterruptedException exception) {
                try {
                    firebaseAuth.deleteUser(created.getUid());
                } catch (FirebaseAuthException ignored) {
                    // Mantém a falha original; a conta órfã pode ser removida manualmente se o Firebase estiver indisponível.
                }
                throw exception;
            }
        }
    }

    private void ensureEmailDoesNotExist(String email) {
        try {
            firebaseAuth.getUserByEmail(email);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        } catch (FirebaseAuthException exception) {
            // USER_NOT_FOUND significa que o e-mail está livre; os outros erros não podem ser tratados como sucesso.
            if (exception.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                if (exception.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
                }
                // Registra códigos técnicos para diagnóstico sem expor e-mail, senha, token ou chave.
                log.warn("Falha ao consultar e-mail no Firebase Authentication: authErrorCode={}, errorCode={}, causeType={}",
                        exception.getAuthErrorCode(), exception.getErrorCode(),
                        exception.getCause() == null ? "none" : exception.getCause().getClass().getSimpleName());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível verificar o e-mail no Firebase Authentication.");
            }
        }
    }

    /** Remove entradas expiradas em toda solicitação, sem depender de um scheduler global. */
    private void cleanupExpiredRegistrations(String emailToKeepForExpirationCheck) {
        Instant now = Instant.now();
        pendingRegistrations.entrySet().removeIf(entry ->
                !entry.getKey().equals(emailToKeepForExpirationCheck)
                        && entry.getValue().expiration().isBefore(now));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static final class PendingRegistration {
        private final CadastroRequestDTO request;
        private final String code;
        private final Instant expiration;
        private int attempts;

        private PendingRegistration(CadastroRequestDTO request, String code, Instant expiration, int attempts) {
            this.request = request;
            this.code = code;
            this.expiration = expiration;
            this.attempts = attempts;
        }

        private CadastroRequestDTO request() { return request; }
        private String code() { return code; }
        private Instant expiration() { return expiration; }
        private int incrementAttempts() { return ++attempts; }
    }
}