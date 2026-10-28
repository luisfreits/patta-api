package com.patta.api.controller;

import com.patta.api.dto.CadastroRequestDTO;
import com.patta.api.dto.EsqueciSenhaRequestDTO;
import com.patta.api.dto.LoginRequestDTO;
import com.patta.api.dto.LoginResponseDTO;
import com.patta.api.dto.MessageResponseDTO;
import com.patta.api.dto.VerificationStatusDTO;
import com.patta.api.service.CadastroService;
import com.patta.api.service.FirebaseEmailVerificationService;
import com.patta.api.service.LoginService;
import com.patta.api.service.RedefinicaoSenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

/**
 * Rotas de cadastro, autenticação e verificação de e-mail.
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final CadastroService cadastroService;
    private final FirebaseEmailVerificationService emailVerificationService;
    private final LoginService loginService;
    private final RedefinicaoSenhaService redefinicaoSenhaService;

    /** POST /cadastro/solicitar: cria a conta e solicita o link nativo do Firebase. Retorna 202. */
    @PostMapping("/cadastro/solicitar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponseDTO solicitarCadastro(@Valid @RequestBody CadastroRequestDTO request)
            throws ExecutionException, InterruptedException {
        cadastroService.solicitarCadastro(request);
        return new MessageResponseDTO(
                "Conta criada. Confira sua caixa de entrada e a pasta de spam para confirmar seu e-mail.");
    }

    /** POST /auth/verificacao/reenviar: reenvia o link para a conta autenticada. */
    @PostMapping("/auth/verificacao/reenviar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponseDTO reenviarVerificacao(
            @AuthenticationPrincipal String uid,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String idToken = authorization.substring("Bearer ".length());
        emailVerificationService.reenviar(uid, idToken);
        return new MessageResponseDTO("Um novo e-mail de verificação foi enviado.");
    }

    /** GET /auth/verificacao/status: retorna o estado atual consultado no Firebase Authentication. */
    @GetMapping("/auth/verificacao/status")
    public VerificationStatusDTO statusVerificacao(@AuthenticationPrincipal String uid) {
        return new VerificationStatusDTO(emailVerificationService.emailVerificado(uid));
    }

    /** POST /auth/login: troca e-mail e senha por tokens do Firebase. */
    @PostMapping("/auth/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return loginService.login(request);
    }

    /**
     * POST /auth/esqueci-senha: pede ao Firebase o e-mail de redefinição.
     * Retorna 202 com a mesma mensagem, exista ou não conta com o e-mail.
     */
    @PostMapping("/auth/esqueci-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponseDTO esqueciSenha(@Valid @RequestBody EsqueciSenhaRequestDTO request) {
        redefinicaoSenhaService.enviarLinkRedefinicao(request);
        return new MessageResponseDTO(
                "Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha.");
    }
}
