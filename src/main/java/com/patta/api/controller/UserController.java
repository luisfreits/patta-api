package com.patta.api.controller;

import com.patta.api.dto.UserResponseDTO;
import com.patta.api.dto.UserUpdateDTO;
import com.patta.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

/** Endpoints para o perfil do próprio usuário, identificado pelo token e não pelo corpo da chamada. */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponseDTO buscarMeuPerfil(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return userService.buscarMeuPerfil(uid);
    }

    @PutMapping
    public UserResponseDTO atualizarMeuPerfil(@AuthenticationPrincipal String uid,
                                               @Valid @RequestBody UserUpdateDTO request)
            throws ExecutionException, InterruptedException {
        return userService.atualizarMeuPerfil(uid, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarMinhaConta(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        userService.deletarMinhaConta(uid);
    }
}