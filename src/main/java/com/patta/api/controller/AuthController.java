package com.patta.api.controller;

import com.patta.api.dto.CadastroRequestDTO;
import com.patta.api.dto.ConfirmacaoRequestDTO;
import com.patta.api.model.User;
import com.patta.api.service.CadastroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CadastroService cadastroService;

    // envia dados do formulário
    @PostMapping("/registrar")
    public ResponseEntity<String> iniciarCadastro(@RequestBody CadastroRequestDTO request) {
        try {
            cadastroService.solicitarCadastro(request);
            return ResponseEntity.ok("Código enviado para o e-mail: " + request.getEmail());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // envia o email e o código que o usuário digitou
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarCadastro(@RequestBody ConfirmacaoRequestDTO request) {
        try {
            User criado = cadastroService.confirmarCadastro(request.getEmail(), request.getCodigo());
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (RuntimeException | ExecutionException | InterruptedException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}