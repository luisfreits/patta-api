package com.patta.api.controller;

import com.patta.api.model.User;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // --- ROTA: Criar Novo Usuário (POST /api/users) ---
    @PostMapping
    public ResponseEntity<User> criar(@RequestBody User user)
            throws ExecutionException, InterruptedException {
        User criado = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    // --- ROTA: Obter Usuário por ID (GET /api/users/{id}) ---
    @GetMapping("/{id}")
    public ResponseEntity<User> buscarPorId(@PathVariable String id)
            throws ExecutionException, InterruptedException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));
        return ResponseEntity.ok(user);
    }

    // --- ROTA: Obter Todos os Usuários (GET /api/users) ---
    @GetMapping
    public ResponseEntity<List<User>> listarTodos()
            throws ExecutionException, InterruptedException {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    // --- ROTA: Atualizar Usuário Existente (PUT /api/users/{id}) ---
    @PutMapping("/{id}")
    public ResponseEntity<User> atualizar(@PathVariable String id, @RequestBody User user)
            throws ExecutionException, InterruptedException {
        user.setId(id);
        User atualizado = userRepository.save(user);
        return ResponseEntity.ok(atualizado);
    }

    // --- ROTA: Excluir Usuário (DELETE /api/users/{id}) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id)
            throws ExecutionException, InterruptedException {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- HANDLER DE ERRO LOCAL ---
    // Intercepta RuntimeException lançadas neste Controller e retorna HTTP status 404 (Not Found)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}