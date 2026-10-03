package com.patta.api.controller;

import com.patta.api.dto.AnimalRequestDTO;
import com.patta.api.dto.AnimalResponseDTO;
import com.patta.api.service.AnimalService;
import com.patta.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/animais")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;
    private final AuthService authService;

    // POST /animais
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public AnimalResponseDTO criar(@RequestHeader("Authorization") String auth,
                                   @RequestBody AnimalRequestDTO dto)
            throws ExecutionException, InterruptedException {
        String userId = authService.identificarUsuario(auth);
        return animalService.criar(userId, dto);
    }

    // GET /animais -> só os animais de quem está logado.
    // Não existe /animais/{userId}: o usuário vem do token, então
    // ninguém consegue listar os animais de outra pessoa.
    @GetMapping
    public List<AnimalResponseDTO> listar(@RequestHeader("Authorization") String auth)
            throws ExecutionException, InterruptedException {
        return animalService.listar(authService.identificarUsuario(auth));
    }

    // GET /animais/{id}
    @GetMapping("/{id}")
    public AnimalResponseDTO buscar(@RequestHeader("Authorization") String auth,
                                    @PathVariable String id)
            throws ExecutionException, InterruptedException {
        return animalService.buscar(authService.identificarUsuario(auth), id);
    }

    // PUT /animais/{id}
    @PutMapping("/{id}")
    public AnimalResponseDTO atualizar(@RequestHeader("Authorization") String auth,
                                       @PathVariable String id,
                                       @RequestBody AnimalRequestDTO dto)
            throws ExecutionException, InterruptedException {
        return animalService.atualizar(authService.identificarUsuario(auth), id, dto);
    }

    // DELETE /animais/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deletar(@RequestHeader("Authorization") String auth,
                        @PathVariable String id)
            throws ExecutionException, InterruptedException {
        animalService.deletar(authService.identificarUsuario(auth), id);
    }
}