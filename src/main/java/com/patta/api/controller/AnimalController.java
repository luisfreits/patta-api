package com.patta.api.controller;

import com.patta.api.dto.AnimalRequestDTO;
import com.patta.api.dto.AnimalResponseDTO;
import com.patta.api.service.AnimalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Rotas de animais. O uid vem do token (@AuthenticationPrincipal), nunca do corpo
 * da requisição, então cada usuário só opera sobre os próprios animais.
 */
@RestController               // Classe que recebe requisições HTTP e devolve JSON
@RequestMapping("/animais")   // Todas as rotas deste controller começam com /animais
@RequiredArgsConstructor      // Lombok gera o construtor com os campos final (injeção por construtor)
public class AnimalController {

    private final AnimalService animalService;

    /** POST /animais: cadastra um animal para o usuário logado. Retorna 201. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnimalResponseDTO criar(@AuthenticationPrincipal String uid,      // uid do usuário autenticado
                                   @Valid @RequestBody AnimalRequestDTO request) // JSON do corpo, validado com @Valid
            throws ExecutionException, InterruptedException {                 // Chamadas assíncronas ao banco podem lançar estas exceções
        return animalService.criar(uid, request);
    }

    /**
     * GET /animais: lista os animais do usuário logado.
     * Os filtros são combinados com AND; nome é busca parcial e espécie é comparação exata.
     */
    @GetMapping
    public List<AnimalResponseDTO> listar(@AuthenticationPrincipal String uid,
                                          @RequestParam(required = false) String nome,
                                          @RequestParam(required = false) String especie)
            throws ExecutionException, InterruptedException {
        return animalService.listar(uid, nome, especie);
    }

    /** GET /animais/{id}: busca um animal. O service confere se ele pertence ao uid. */
    @GetMapping("/{id}")
    public AnimalResponseDTO buscar(@AuthenticationPrincipal String uid,
                                    @PathVariable String id)                  // Id vindo da URL
            throws ExecutionException, InterruptedException {
        return animalService.buscar(uid, id);
    }

    /** PUT /animais/{id}: substitui os dados de um animal. Retorna 200 com o animal atualizado. */
    @PutMapping("/{id}")
    public AnimalResponseDTO atualizar(@AuthenticationPrincipal String uid,
                                       @PathVariable String id,
                                       @Valid @RequestBody AnimalRequestDTO request)
            throws ExecutionException, InterruptedException {
        return animalService.atualizar(uid, id, request);
    }

    /** DELETE /animais/{id}: remove um animal. Retorna 204 (sem corpo na resposta). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@AuthenticationPrincipal String uid,
                        @PathVariable String id)
            throws ExecutionException, InterruptedException {
        animalService.deletar(uid, id);
    }
}
