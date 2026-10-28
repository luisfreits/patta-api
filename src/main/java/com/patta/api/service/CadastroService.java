package com.patta.api.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.patta.api.dto.CadastroRequestDTO;
import com.patta.api.model.UserModel;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Cria a identidade no Firebase Authentication e o perfil correspondente no Firestore.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CadastroService {

    private final UserRepository userRepository;
    private final FirebaseEmailVerificationService emailVerificationService;
    private final FirebaseAuth firebaseAuth;

    /**
     * Cria uma conta não verificada, persiste o perfil e solicita ao Firebase o envio
     * do link de verificação. A confirmação do link é concluída pelo Firebase Auth.
     */
    public void solicitarCadastro(CadastroRequestDTO dados) throws ExecutionException, InterruptedException {
        String email = normalizarEmail(dados.email());
        verificarEmailLivre(email);

        UserRecord criado = criarIdentidade(email, dados.senha());
        UserModel usuario = UserModel.builder()
                .id(criado.getUid())
                .nome(dados.nome().trim())
                .email(email)
                .telefone(dados.telefone().trim())
                .build();
        try {
            userRepository.save(usuario);
        } catch (ExecutionException | InterruptedException exception) {
            desfazerIdentidade(criado.getUid());
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw exception;
        }

        try {
            emailVerificationService.enviarParaNovaConta(email, dados.senha());
        } catch (ResponseStatusException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A conta foi criada, mas não foi possível enviar o e-mail de verificação. "
                            + "Entre e solicite um novo envio.", exception);
        }
    }

    private UserRecord criarIdentidade(String email, String senha) {
        try {
            return firebaseAuth.createUser(new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setEmailVerified(false)
                    .setPassword(senha));
        } catch (FirebaseAuthException exception) {
            // Comparação com o enum tipado; getErrorCode() devolve texto, não AuthErrorCode
            if (exception.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
            }
            log.warn("Falha ao criar usuário no Firebase Authentication: authErrorCode={}", exception.getAuthErrorCode());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível criar a conta no Firebase Authentication.");
        }
    }

    // Apaga a conta do Firebase Auth quando o perfil não pôde ser salvo
    private void desfazerIdentidade(String uid) {
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException exception) {
            // Não é possível desfazer agora: registra o uid para remoção manual
            log.error("Conta órfã no Firebase Authentication, remover manualmente. uid={}", uid, exception);
        }
    }

    /**
     * Confere se o e-mail não está cadastrado no Firebase Auth.
     * USER_NOT_FOUND significa e-mail livre. Qualquer outro erro bloqueia o cadastro.
     */
    private void verificarEmailLivre(String email) {
        try {
            firebaseAuth.getUserByEmail(email);
        } catch (FirebaseAuthException exception) {
            if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                return;
            }
            // Registra só códigos técnicos; nunca e-mail, senha ou token
            log.warn("Falha ao consultar e-mail no Firebase Authentication: authErrorCode={}", exception.getAuthErrorCode());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível verificar o e-mail no Firebase Authentication.");
        }
        // Não houve exceção: o usuário existe
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
