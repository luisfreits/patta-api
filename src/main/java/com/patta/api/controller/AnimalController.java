package com.patta.api.controller;

import com.patta.api.dto.AnimalRequestDTO;
import com.patta.api.dto.AnimalResponseDTO;
import com.patta.api.service.AnimalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

/** Rotas protegidas de animais; o token define o dono em todas as operações. */
@RestController // Indica que a classe recebe requisições HTTP e devolve JSON
@RequestMapping("/animais") // Todas as rotas daqui começam com /animais
@RequiredArgsConstructor // O Lombok cria o construtor com os campos final (injeção do service)
public class AnimalController {

    // Service que tem a lógica de negócio dos animais
    private final AnimalService animalService;

    // POST /animais -> cadastra um novo animal
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Devolve status 201 (criado) em vez de 200
    public AnimalResponseDTO criar(@AuthenticationPrincipal String uid, // uid do usuário logado, vem do token
                                   @Valid @RequestBody AnimalRequestDTO request) // Lê o JSON do corpo e valida os campos
            throws ExecutionException, InterruptedException { // Exceções que podem vir da chamada assíncrona ao banco
        // Passa o uid pro service saber de quem é o animal
        return animalService.criar(uid, request);
    }

    // GET /animais -> lista os animais do usuário logado
    @GetMapping
    public List<AnimalResponseDTO> listar(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        // Só traz os animais desse usuário
        return animalService.listar(uid);
    }

    // GET /animais/{id} -> busca um animal específico pelo id
    @GetMapping("/{id}")
    public AnimalResponseDTO buscar(@AuthenticationPrincipal String uid,
                                    @PathVariable String id) // Pega o id que vem na URL
            throws ExecutionException, InterruptedException {
        // Usa o uid também pra garantir que o animal é do usuário
        return animalService.buscar(uid, id);
    }

    // PUT /animais/{id} -> atualiza os dados de um animal
    @PutMapping("/{id}")
    public AnimalResponseDTO atualizar(@AuthenticationPrincipal String uid,
                                       @PathVariable String id, // id do animal na URL
                                       @Valid @RequestBody AnimalRequestDTO request) // Novos dados, já validados
            throws ExecutionException, InterruptedException {
        return animalService.atualizar(uid, id, request);
    }

    // DELETE /animais/{id} -> apaga um animal
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Devolve status 204 (sem conteúdo), pois não retorna nada
    public void deletar(@AuthenticationPrincipal String uid, @PathVariable String id)
            throws ExecutionException, InterruptedException {
        animalService.deletar(uid, id);
    }
}