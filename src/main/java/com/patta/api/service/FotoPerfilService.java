package com.patta.api.service;

import com.patta.api.model.UserModel;
import com.patta.api.repository.ImagemRepository;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class FotoPerfilService {

    static final int TAMANHO_MAXIMO = 700 * 1024;

    private final ImagemRepository imagemRepository;
    private final UserRepository userRepository;
    private final PapelUsuarioService papelUsuarioService;

    /** Valida e grava a foto do usuário, e aponta o fotoPerfil para a rota da imagem. */
    public String salvar(String uid, MultipartFile arquivo)
            throws IOException, ExecutionException, InterruptedException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "A imagem deve ter no máximo 700 KB.");
        }
        byte[] dados = arquivo.getBytes();
        String tipo = detectarTipo(dados);
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Use uma imagem JPEG, PNG ou WebP.");
        }
        UserModel usuario = buscarUsuario(uid);
        imagemRepository.save(uid, tipo, dados);
        String caminho = "/users/" + uid + "/foto";
        usuario.setFotoPerfil(caminho);
        userRepository.save(usuario);
        return caminho;
    }

    /** Devolve a foto. Só o dono, administradores e profissionais podem ver. */
    public ImagemRepository.Imagem buscar(String solicitanteUid, String donoUid)
            throws ExecutionException, InterruptedException {
        boolean permitido = solicitanteUid.equals(donoUid)
                || papelUsuarioService.ehAdministrador(solicitanteUid)
                || papelUsuarioService.ehProfissional(solicitanteUid);
        if (!permitido) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        return imagemRepository.findById(donoUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada."));
    }

    /** Remove a foto e volta para a imagem padrão. */
    public void remover(String uid) throws ExecutionException, InterruptedException {
        UserModel usuario = buscarUsuario(uid);
        imagemRepository.deleteById(uid);
        usuario.setFotoPerfil(UserModel.FOTO_PERFIL_PADRAO);
        userRepository.save(usuario);
    }

    private UserModel buscarUsuario(String uid) throws ExecutionException, InterruptedException {
        return userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    /** Descobre o tipo pelos primeiros bytes, sem confiar no Content-Type enviado. */
    static String detectarTipo(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
