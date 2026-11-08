package com.patta.api.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.patta.api.model.ClinicaConfigModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class ClinicaConfigRepository {

    private static final String COLECAO = "configuracao";
    private static final String DOCUMENTO = "clinica";

    private final Firestore firestore;

    public ClinicaConfigModel buscar() throws ExecutionException, InterruptedException {
        DocumentSnapshot documento = referencia().get().get();
        if (!documento.exists()) {
            return ClinicaConfigModel.builder().aberta(true).build();
        }
        ClinicaConfigModel config = documento.toObject(ClinicaConfigModel.class);
        if (config == null) {
            return ClinicaConfigModel.builder().aberta(true).build();
        }
        if (config.getAberta() == null) {
            config.setAberta(true);
        }
        return config;
    }

    public ClinicaConfigModel alterarEstado(String uid, boolean aberta)
            throws ExecutionException, InterruptedException {
        DocumentReference documento = referencia();
        try {
            return firestore.runTransaction(transacao -> {
                DocumentSnapshot atual = transacao.get(documento).get();
                boolean estadoAtual = !atual.exists() || !Boolean.FALSE.equals(atual.getBoolean("aberta"));
                if (estadoAtual == aberta) {
                    throw new EstadoClinicaInalteradoException();
                }

                Date atualizadoEm = new Date();
                Map<String, Object> campos = new HashMap<>();
                campos.put("aberta", aberta);
                campos.put("atualizadoPor", uid);
                campos.put("atualizadoEm", atualizadoEm);
                transacao.set(documento, campos, SetOptions.merge());

                return ClinicaConfigModel.builder()
                        .aberta(aberta)
                        .atualizadoPor(uid)
                        .atualizadoEm(atualizadoEm)
                        .build();
            }).get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof EstadoClinicaInalteradoException conflito) {
                throw conflito;
            }
            throw exception;
        }
    }

    public ClinicaConfigModel atualizarCapacidadeHospedagem(int capacidadeMaxima)
            throws ExecutionException, InterruptedException {
        Map<String, Object> campos = Map.of("capacidadeHospedagem", capacidadeMaxima);
        referencia().set(campos, SetOptions.merge()).get();
        return buscar();
    }

    private DocumentReference referencia() {
        return firestore.collection(COLECAO).document(DOCUMENTO);
    }

    public static final class EstadoClinicaInalteradoException extends RuntimeException {
    }
}
