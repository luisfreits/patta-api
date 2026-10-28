package com.patta.api.service;

import com.patta.api.dto.AnimalRequestDTO;
import com.patta.api.dto.AnimalResponseDTO;
import com.patta.api.model.AnimalModel;
import com.patta.api.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Regras de negócio dos animais. Cada operação confere se o animal pertence ao usuário logado.
 */
@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;

    /** Cria um animal. O dono é o uid autenticado, nunca um campo enviado pelo cliente. */
    public AnimalResponseDTO criar(String uid, AnimalRequestDTO dados) throws ExecutionException, InterruptedException {
        AnimalModel animal = AnimalModel.builder()
                .userId(uid)
                .nome(dados.nome().trim())
                .sexo(dados.sexo().trim())
                .idade(dados.idade())
                .animal(dados.animal().trim())
                .castrado(dados.castrado())
                .build();
        return toResponse(animalRepository.save(animal));
    }

    /**
     * Busca apenas os documentos do usuário no Firestore e aplica filtros combinados
     * sobre esse conjunto. Nome aceita trecho; espécie exige igualdade sem diferenciar
     * maiúsculas/minúsculas.
     */
    public List<AnimalResponseDTO> listar(String uid, String nome, String especie)
            throws ExecutionException, InterruptedException {
        String filtroNome = normalizarFiltro(nome, 100, "nome");
        String filtroEspecie = normalizarFiltro(especie, 60, "espécie");
        return animalRepository.findByUserId(uid).stream()
                .filter(animal -> uid.equals(animal.getUserId()))
                .filter(animal -> filtroNome == null
                        || animal.getNome() != null
                        && animal.getNome().toLowerCase(Locale.ROOT).contains(filtroNome))
                .filter(animal -> filtroEspecie == null
                        || animal.getAnimal() != null
                        && animal.getAnimal().equalsIgnoreCase(filtroEspecie))
                .map(this::toResponse)
                .toList();
    }

    /** Busca um animal do usuário. */
    public AnimalResponseDTO buscar(String uid, String animalId) throws ExecutionException, InterruptedException {
        return toResponse(buscarDoUsuario(uid, animalId));
    }

    /** Atualiza os campos editáveis. O id e o dono do documento não mudam. */
    public AnimalResponseDTO atualizar(String uid, String animalId, AnimalRequestDTO dados)
            throws ExecutionException, InterruptedException {
        AnimalModel animal = buscarDoUsuario(uid, animalId);
        animal.setNome(dados.nome().trim());
        animal.setSexo(dados.sexo().trim());
        animal.setIdade(dados.idade());
        animal.setAnimal(dados.animal().trim());
        animal.setCastrado(dados.castrado());
        return toResponse(animalRepository.save(animal));
    }

    /** Remove um animal do usuário. */
    public void deletar(String uid, String animalId) throws ExecutionException, InterruptedException {
        buscarDoUsuario(uid, animalId); // Lança 404 se não existir ou não for do usuário
        animalRepository.deleteById(animalId);
    }

    /**
     * Busca o animal e confere o dono. Animais de outros usuários recebem 404,
     * o mesmo erro de um id inexistente, para não revelar quais ids existem.
     */
    private AnimalModel buscarDoUsuario(String uid, String animalId)
            throws ExecutionException, InterruptedException {
        return animalRepository.findById(animalId)
                .filter(animal -> uid.equals(animal.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado."));
    }

    private String normalizarFiltro(String valor, int tamanhoMaximo, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String filtro = valor.trim().toLowerCase(Locale.ROOT);
        if (filtro.length() > tamanhoMaximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O filtro de " + campo + " deve ter no máximo " + tamanhoMaximo + " caracteres.");
        }
        return filtro;
    }

    // Converte a entidade em DTO. O userId não sai na resposta.
    private AnimalResponseDTO toResponse(AnimalModel animal) {
        return AnimalResponseDTO.builder()
                .id(animal.getId())
                .nome(animal.getNome())
                .sexo(animal.getSexo())
                .idade(animal.getIdade())
                .animal(animal.getAnimal())
                .castrado(animal.getCastrado())
                .build();
    }
}
