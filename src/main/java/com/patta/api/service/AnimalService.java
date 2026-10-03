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
import java.util.concurrent.ExecutionException;

// @RequiredArgsConstructor (Lombok) gera o construtor com os campos "final",
// e o Spring injeta o repository automaticamente, igual no seu CadastroService.
@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;

    // CRIAR -----------------------------------------------------------
    public AnimalResponseDTO criar(String userId, AnimalRequestDTO dto)
            throws ExecutionException, InterruptedException {

        // Monta o Model com o builder (mesmo estilo do UserModel).
        AnimalModel animal = AnimalModel.builder()
                .userId(userId)   // <- A CONEXÃO: carimbamos o dono aqui
                .nome(dto.getNome())
                .sexo(dto.getSexo())
                .idade(dto.getIdade())
                .animal(dto.getAnimal())
                .castrado(dto.getCastrado())
                .build();

        // O repository preenche o id gerado pelo Firestore.
        return paraResponse(animalRepository.save(animal));
    }

    // LISTAR os animais do usuário logado ------------------------------
    public List<AnimalResponseDTO> listar(String userId)
            throws ExecutionException, InterruptedException {

        return animalRepository.findByUserId(userId)
                .stream()
                .map(this::paraResponse)  // Model -> DTO, um por um
                .toList();
    }

    // BUSCAR um animal (com checagem de dono) --------------------------
    public AnimalResponseDTO buscar(String userId, String animalId)
            throws ExecutionException, InterruptedException {
        return paraResponse(buscarEValidarDono(userId, animalId));
    }

    // ATUALIZAR (com checagem de dono) ---------------------------------
    public AnimalResponseDTO atualizar(String userId, String animalId, AnimalRequestDTO dto)
            throws ExecutionException, InterruptedException {

        // Garante que o animal existe e é do usuário ANTES de alterar.
        AnimalModel animal = buscarEValidarDono(userId, animalId);

        // Só mexemos nos campos editáveis; id e userId permanecem intactos.
        animal.setNome(dto.getNome());
        animal.setSexo(dto.getSexo());
        animal.setIdade(dto.getIdade());
        animal.setAnimal(dto.getAnimal());
        animal.setCastrado(dto.getCastrado());

        // Como o id está preenchido, o save sobrescreve o documento existente.
        return paraResponse(animalRepository.save(animal));
    }

    // DELETAR (com checagem de dono) -----------------------------------
    public void deletar(String userId, String animalId)
            throws ExecutionException, InterruptedException {
        buscarEValidarDono(userId, animalId); // lança erro se não for dono
        animalRepository.deleteById(animalId);
    }

    // AUXILIARES -------------------------------------------------------

    // Reutilizado por buscar, atualizar e deletar.
    private AnimalModel buscarEValidarDono(String userId, String animalId)
            throws ExecutionException, InterruptedException {

        AnimalModel animal = animalRepository.findById(animalId);

        if (animal == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado");
        }

        // Sem esta checagem, qualquer usuário logado mexeria no animal
        // de outro só por adivinhar o ID.
        if (!animal.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este animal não é seu");
        }

        return animal;
    }

    private AnimalResponseDTO paraResponse(AnimalModel a) {
        return AnimalResponseDTO.builder()
                .id(a.getId())
                .nome(a.getNome())
                .sexo(a.getSexo())
                .idade(a.getIdade())
                .animal(a.getAnimal())
                .castrado(a.getCastrado())
                .build();
    }
}