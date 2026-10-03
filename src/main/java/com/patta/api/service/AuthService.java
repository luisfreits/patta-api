package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Responsabilidade única: transformar a requisição no id do usuário logado.
// Se você mudar a forma de login (ex.: JWT próprio), só esta classe muda.
@Service
public class AuthService {

    public String identificarUsuario(String authHeader) {
        // Valida o formato esperado: "Bearer <token>"
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token ausente");
        }

        String token = authHeader.substring(7); // remove "Bearer "

        try {
            // O Firebase valida assinatura, validade e projeto do token.
            // Se válido, getUid() é o id do usuário.
            return FirebaseAuth.getInstance().verifyIdToken(token).getUid();
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}