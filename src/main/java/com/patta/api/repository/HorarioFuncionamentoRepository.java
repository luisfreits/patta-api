package com.patta.api.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.patta.api.model.HorarioFuncionamentoModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class HorarioFuncionamentoRepository {

    private static final String COLECAO = "horariosFuncionamento";
    private final Firestore firestore;

    public HorarioFuncionamentoModel save(HorarioFuncionamentoModel horario)
            throws ExecutionException, InterruptedException {
        DocumentReference documento = horarios().document(horario.getId());
        documento.set(horario).get();
        return horario;
    }

    public Optional<HorarioFuncionamentoModel> findByServicoAndDia(String servico, String diaSemana)
            throws ExecutionException, InterruptedException {
        String id = id(servico, diaSemana);
        DocumentSnapshot documento = horarios().document(id).get().get();
        if (!documento.exists()) {
            return Optional.empty();
        }
        HorarioFuncionamentoModel model = documento.toObject(HorarioFuncionamentoModel.class);
        if (model != null) {
            model.setId(documento.getId());
        }
        return Optional.ofNullable(model);
    }

    public void delete(String servico, String diaSemana) throws ExecutionException, InterruptedException {
        horarios().document(id(servico, diaSemana)).delete().get();
    }

    public static String id(String servico, String diaSemana) {
        return servico + "_" + diaSemana;
    }

    private CollectionReference horarios() {
        return firestore.collection(COLECAO);
    }
}
