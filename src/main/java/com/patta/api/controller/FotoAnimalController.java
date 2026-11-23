package com.patta.api.controller;

import com.patta.api.repository.ImagemRepository;
import com.patta.api.service.FotoAnimalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Foto do pet: envio, leitura e remoção. */
@RestController
@RequestMapping("/animais")
@RequiredArgsConstructor
public class FotoAnimalController {

    private final FotoAnimalService fotoAnimalService;

    /** PUT /animais/{id}/foto (multipart, campo "arquivo"). */
    @PutMapping(value = "/{animalId}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Boolean> enviar(@AuthenticationPrincipal String uid,
                                       @PathVariable String animalId,
                                       @RequestParam("arquivo") MultipartFile arquivo)
            throws IOException, ExecutionException, InterruptedException {
        fotoAnimalService.salvar(uid, animalId, arquivo);
        return Map.of("temFoto", true);
    }

    /** GET /animais/{id}/foto. Exige token. */
    @GetMapping("/{animalId}/foto")
    public ResponseEntity<byte[]> buscar(@AuthenticationPrincipal String uid,
                                         @PathVariable String animalId)
            throws ExecutionException, InterruptedException {
        ImagemRepository.Imagem imagem = fotoAnimalService.buscar(uid, animalId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(imagem.dados());
    }

    /** DELETE /animais/{id}/foto. */
    @DeleteMapping("/{animalId}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal String uid, @PathVariable String animalId)
            throws ExecutionException, InterruptedException {
        fotoAnimalService.remover(uid, animalId);
    }
}
