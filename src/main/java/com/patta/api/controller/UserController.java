package com.patta.api.controller;

import com.patta.api.dto.UserResponseDTO;
import com.patta.api.dto.UserUpdateDTO;
import com.patta.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

/**
 * Perfil do próprio usuário. A rota é /users/me: o "me" indica que o dono
 * é quem está logado, identificado pelo token e nunca por um id na URL.
 */
@RestController
@RequestMapping("/users/me")  // Todas as rotas começam com /users/me
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /users/me: retorna os dados do usuário logado. */
    @GetMapping
    public UserResponseDTO buscarMeuPerfil(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return userService.buscarMeuPerfil(uid);
    }

    /** PUT /users/me: atualiza nome e telefone. E-mail e uid não podem ser alterados. */
    @PutMapping
    public UserResponseDTO atualizarMeuPerfil(@AuthenticationPrincipal String uid,
                                              @Valid @RequestBody UserUpdateDTO request)
            throws ExecutionException, InterruptedException {
        return userService.atualizarMeuPerfil(uid, request);
    }

    /** DELETE /users/me: apaga a conta do usuário logado. Retorna 204. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarMinhaConta(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        userService.deletarMinhaConta(uid);
    }
}
