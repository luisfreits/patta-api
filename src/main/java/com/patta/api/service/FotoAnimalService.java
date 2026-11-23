package com.patta.api.service;

import com.patta.api.model.AnimalModel;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.ImagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Foto do pet guardada no Firestore (coleção "imagens", id "animal_{animalId}").
 * Mesmas regras da foto de perfil: JPEG, PNG ou WebP, até 700 KB.
 */
@Service
@RequiredArgsConstructor
public class FotoAnimalService {

    private final ImagemRepository imagemRepository;
    private final AnimalRepository animalRepository;
    private final PapelUsuarioService papelUsuarioService;

    /** Chave do documento da imagem. Usada também ao excluir o animal ou a conta. */
    public static String chave(String animalId) {
        return "animal_" + animalId;
    }

    public void salvar(String uid, String animalId, MultipartFile arquivo)
            throws IOException, ExecutionException, InterruptedException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem.");
        }
        if (arquivo.getSize() > FotoPerfilService.TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "A imagem deve ter no máximo 700 KB.");
        }
        byte[] dados = arquivo.getBytes();
        String tipo = FotoPerfilService.detectarTipo(dados);
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Use uma imagem JPEG, PNG ou WebP.");
        }
        AnimalModel animal = buscarDoUsuario(uid, animalId);
        imagemRepository.save(chave(animalId), tipo, dados);
        animal.setTemFoto(true);
        animalRepository.save(animal);
    }

    /** Devolve a foto. Só o dono, administradores e profissionais podem ver. */
    public ImagemRepository.Imagem buscar(String uid, String animalId)
            throws ExecutionException, InterruptedException {
        AnimalModel animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado."));
        boolean permitido = uid.equals(animal.getUserId())
                || papelUsuarioService.ehAdministrador(uid)
                || papelUsuarioService.ehProfissional(uid);
        if (!permitido) {
            // Mesmo erro de um id inexistente, para não revelar quais ids existem.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado.");
        }
        return imagemRepository.findById(chave(animalId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada."));
    }

    public void remover(String uid, String animalId) throws ExecutionException, InterruptedException {
        AnimalModel animal = buscarDoUsuario(uid, animalId);
        imagemRepository.deleteById(chave(animalId));
        animal.setTemFoto(false);
        animalRepository.save(animal);
    }

    private AnimalModel buscarDoUsuario(String uid, String animalId)
            throws ExecutionException, InterruptedException {
        return animalRepository.findById(animalId)
                .filter(animal -> papelUsuarioService.ehAdministrador(uid) || uid.equals(animal.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado."));
    }
}
