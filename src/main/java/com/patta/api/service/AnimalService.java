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

/** Regras de negócio e autorização por proprietário para os animais. */
@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;

    public AnimalResponseDTO criar(String authenticatedUid, AnimalRequestDTO request)
            throws ExecutionException, InterruptedException {
        // O vínculo é definido aqui pelo principal autenticado, nunca por um campo enviado pelo cliente.
        AnimalModel animal = AnimalModel.builder()
                .userId(authenticatedUid)
                .nome(request.getNome().trim())
                .sexo(request.getSexo().trim())
                .idade(request.getIdade())
                .animal(request.getAnimal().trim())
                .castrado(request.getCastrado())
                .build();
        return toResponse(animalRepository.save(animal));
    }

    public List<AnimalResponseDTO> listar(String authenticatedUid)
            throws ExecutionException, InterruptedException {
        // O repository aplica whereEqualTo(userId, uid), isolando a lista no Firestore.
        return animalRepository.findByUserId(authenticatedUid).stream().map(this::toResponse).toList();
    }

    public AnimalResponseDTO buscar(String authenticatedUid, String animalId)
            throws ExecutionException, InterruptedException {
        return toResponse(findOwnedAnimal(authenticatedUid, animalId));
    }

    public AnimalResponseDTO atualizar(String authenticatedUid, String animalId, AnimalRequestDTO request)
            throws ExecutionException, InterruptedException {
        AnimalModel animal = findOwnedAnimal(authenticatedUid, animalId);
        // Só os campos editáveis mudam; id e userId continuam ligados ao mesmo documento e dono.
        animal.setNome(request.getNome().trim());
        animal.setSexo(request.getSexo().trim());
        animal.setIdade(request.getIdade());
        animal.setAnimal(request.getAnimal().trim());
        animal.setCastrado(request.getCastrado());
        return toResponse(animalRepository.save(animal));
    }

    public void deletar(String authenticatedUid, String animalId)
            throws ExecutionException, InterruptedException {
        findOwnedAnimal(authenticatedUid, animalId);
        animalRepository.deleteById(animalId);
    }

    private AnimalModel findOwnedAnimal(String authenticatedUid, String animalId)
            throws ExecutionException, InterruptedException {
        AnimalModel animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado."));
        if (!authenticatedUid.equals(animal.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este animal pertence a outro usuário.");
        }
        return animal;
    }

    // O DTO evita que detalhes internos, em especial userId, sejam expostos na resposta HTTP.
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