package com.patta.api.controller;

import com.patta.api.dto.CadastroRequestDTO;
import com.patta.api.dto.ConfirmacaoRequestDTO;
import com.patta.api.dto.LoginRequestDTO;
import com.patta.api.dto.LoginResponseDTO;
import com.patta.api.dto.MessageResponseDTO;
import com.patta.api.dto.UserResponseDTO;
import com.patta.api.model.UserModel;
import com.patta.api.service.CadastroService;
import com.patta.api.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

/** Rotas públicas que encaminham as operações de autenticação e cadastro aos respectivos services. */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final CadastroService cadastroService;
    private final LoginService loginService;

    @PostMapping("/cadastro/solicitar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponseDTO solicitarCadastro(@Valid @RequestBody CadastroRequestDTO request) {
        cadastroService.solicitarCadastro(request);
        return new MessageResponseDTO("Código de confirmação enviado para o e-mail informado.");
    }

    @PostMapping("/cadastro/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO confirmarCadastro(@Valid @RequestBody ConfirmacaoRequestDTO request)
            throws ExecutionException, InterruptedException {
        UserModel user = cadastroService.confirmarCadastro(request.getEmail(), request.getCodigo());
        return UserResponseDTO.builder()
                .id(user.getId())
                .nome(user.getNome())
                .email(user.getEmail())
                .telefone(user.getTelefone())
                .build();
    }

    @PostMapping("/auth/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return loginService.login(request);
    }
}