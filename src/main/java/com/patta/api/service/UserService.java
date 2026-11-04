package com.patta.api.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.patta.api.dto.UserResponseDTO;
import com.patta.api.dto.UserUpdateDTO;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import com.patta.api.repository.RegistroClinicoRepository;
import com.patta.api.repository.VacinaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Regras do perfil do usuário. Todo método recebe o uid já validado pelo filtro do Firebase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final RegistroClinicoRepository registroClinicoRepository;
    private final VacinaRepository vacinaRepository;
    private final FirebaseAuth firebaseAuth;
    private final LoginService loginService;
    private final EmailService emailService;
    private final PapelUsuarioService papelUsuarioService;

    /** Devolve o perfil do usuário logado. */
    public UserResponseDTO buscarMeuPerfil(String uid) throws ExecutionException, InterruptedException {
        return toResponse(buscarUsuario(uid));
    }

    /** Atualiza o perfil e exige a senha atual para trocar o e-mail. */
    public UserResponseDTO atualizarMeuPerfil(String uid, UserUpdateDTO dados)
            throws ExecutionException, InterruptedException {
        return atualizarPerfil(uid, dados, true);
    }

    public UserResponseDTO atualizarPerfilComoAdministrador(String administradorUid, String uid,
                                                             UserUpdateDTO dados)
            throws ExecutionException, InterruptedException {
        exigirAdministrador(administradorUid);
        return atualizarPerfil(uid, dados, false);
    }

    private UserResponseDTO atualizarPerfil(String uid, UserUpdateDTO dados, boolean exigirSenhaAtual)
            throws ExecutionException, InterruptedException {
        UserModel usuario = buscarUsuario(uid);
        UserModel original = copiar(usuario);
        String novoEmail = normalizarEmail(dados.email());
        boolean emailAlterado = !novoEmail.equalsIgnoreCase(usuario.getEmail());
        boolean emailVerificadoOriginal = false;

        if (emailAlterado) {
            if (exigirSenhaAtual) {
                if (dados.senhaAtual() == null || dados.senhaAtual().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Informe sua senha atual para alterar o e-mail.");
                }
                loginService.validarSenhaAtual(uid, usuario.getEmail(), dados.senhaAtual());
            }
            try {
                emailVerificadoOriginal = firebaseAuth.getUser(uid).isEmailVerified();
            } catch (FirebaseAuthException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível consultar a conta no Firebase Authentication.");
            }
            atualizarEmailFirebase(uid, novoEmail, false);
        }

        usuario.setNome(dados.nome().trim());
        usuario.setTelefone(dados.telefone().trim());
        usuario.setEmail(novoEmail);
        try {
            userRepository.save(usuario);
        } catch (ExecutionException | InterruptedException exception) {
            if (emailAlterado) restaurarPerfil(original, emailVerificadoOriginal);
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw exception;
        }

        if (emailAlterado) {
            try {
                String link = firebaseAuth.generateEmailVerificationLink(novoEmail);
                emailService.enviarLinkVerificacao(novoEmail, link);
            } catch (FirebaseAuthException | MailException exception) {
                restaurarPerfil(original, emailVerificadoOriginal);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Não foi possível enviar a verificação. A alteração do e-mail foi revertida.", exception);
            } catch (RuntimeException exception) {
                restaurarPerfil(original, emailVerificadoOriginal);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Não foi possível enviar a verificação. A alteração do e-mail foi revertida.", exception);
            }
        }
        return toResponse(usuario);
    }

    private void atualizarEmailFirebase(String uid, String email, boolean emailVerificado) {
        try {
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(uid)
                    .setEmail(email)
                    .setEmailVerified(emailVerificado));
        } catch (FirebaseAuthException exception) {
            if (exception.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
            }
            log.warn("Falha ao atualizar e-mail no Firebase Authentication: authErrorCode={}",
                    exception.getAuthErrorCode());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível atualizar o e-mail no Firebase Authentication.");
        }
    }

    private void restaurarPerfil(UserModel original, boolean emailVerificado) {
        try {
            userRepository.save(original);
            atualizarEmailFirebase(original.getId(), original.getEmail(), emailVerificado);
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Falha ao reverter a alteração de perfil. uid={}", original.getId(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível reverter a alteração de e-mail. Entre em contato com o suporte.", exception);
        }
    }

    private UserModel copiar(UserModel usuario) {
        return UserModel.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .fotoPerfil(usuario.getFotoPerfil())
                .build();
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Apaga a conta: primeiro os animais e o documento do usuário no Firestore,
     * depois o usuário no Firebase Auth. Se o Firebase Auth falhar, os dados já foram
     * apagados, então o erro 502 informa que a conta precisa ser removida novamente.
     */
    public void deletarMinhaConta(String uid) throws ExecutionException, InterruptedException {
        deletarConta(uid);
    }

    private void deletarConta(String uid) throws ExecutionException, InterruptedException {
        buscarUsuario(uid); // Confirma que o usuário existe antes de apagar qualquer coisa
        // O repository divide a exclusão em lotes quando há muitos documentos (limite de 500 operações por batch no Firestore)
        animalRepository.deleteByUserId(uid);
        agendamentoRepository.deleteByUsuarioId(uid);
        registroClinicoRepository.deleteByUsuarioId(uid);
        vacinaRepository.deleteByUsuarioId(uid);
        userRepository.deleteById(uid);
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Os dados foram removidos, mas não foi possível excluir a conta no Firebase Authentication.");
        }
    }

    private void exigirAdministrador(String uid) {
        if (!papelUsuarioService.ehAdministrador(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores autorizados podem gerenciar contas.");
        }
    }

    // Busca o usuário pelo uid; 404 se o documento não existir no Firestore
    private UserModel buscarUsuario(String uid) throws ExecutionException, InterruptedException {
        return userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    // Converte a entidade em DTO, sem expor nada além dos campos públicos
    private UserResponseDTO toResponse(UserModel usuario) {
        return UserResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .fotoPerfil(usuario.getFotoPerfil())
                .build();
    }
}
