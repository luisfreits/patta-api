package com.patta.api.repository;

import com.google.cloud.firestore.Blob;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Acesso à coleção "imagens" no Firestore. O id do documento é o uid do dono,
 * então cada usuário tem no máximo uma foto de perfil.
 */
@Repository
@RequiredArgsConstructor
public class ImagemRepository {

    private static final String COLECAO = "imagens";

    private final Firestore firestore;

    /** Imagem já lida do banco. */
    public record Imagem(String contentType, byte[] dados) {}

    public void save(String uid, String contentType, byte[] dados)
            throws ExecutionException, InterruptedException {
        Map<String, Object> documento = new HashMap<>();
        documento.put("contentType", contentType);
        documento.put("tamanho", dados.length);
        documento.put("dados", Blob.fromBytes(dados));
        documento.put("atualizadaEm", System.currentTimeMillis());
        firestore.collection(COLECAO).document(uid).set(documento).get();
    }

    public Optional<Imagem> findById(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot documento = firestore.collection(COLECAO).document(uid).get().get();
        if (!documento.exists()) {
            return Optional.empty();
        }
        Blob blob = documento.getBlob("dados");
        String contentType = documento.getString("contentType");
        if (blob == null || contentType == null) {
            return Optional.empty();
        }
        return Optional.of(new Imagem(contentType, blob.toBytes()));
    }

    public void deleteById(String uid) throws ExecutionException, InterruptedException {
        firestore.collection(COLECAO).document(uid).delete().get();
    }
}
