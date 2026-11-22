package com.patta.api.controller;

import com.patta.api.repository.ImagemRepository;
import com.patta.api.service.FotoPerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Foto de perfil: envio, leitura e remoção. */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class FotoPerfilController {

    private final FotoPerfilService fotoPerfilService;

    /** PUT /users/me/foto (multipart, campo "arquivo"). Retorna o novo fotoPerfil. */
    @PutMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> enviar(@AuthenticationPrincipal String uid,
                                      @RequestParam("arquivo") MultipartFile arquivo)
            throws IOException, ExecutionException, InterruptedException {
        return Map.of("fotoPerfil", fotoPerfilService.salvar(uid, arquivo));
    }

    /** GET /users/{uid}/foto ("me" vale para o próprio usuário). Exige token. */
    @GetMapping("/{donoUid}/foto")
    public ResponseEntity<byte[]> buscar(@AuthenticationPrincipal String uid,
                                         @PathVariable String donoUid)
            throws ExecutionException, InterruptedException {
        String alvo = "me".equals(donoUid) ? uid : donoUid;
        ImagemRepository.Imagem imagem = fotoPerfilService.buscar(uid, alvo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(imagem.dados());
    }

    /** DELETE /users/me/foto: volta para a imagem padrão. */
    @DeleteMapping("/me/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        fotoPerfilService.remover(uid);
    }
}
