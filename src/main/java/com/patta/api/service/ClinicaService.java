package com.patta.api.service;

import com.patta.api.dto.ClinicaStatusDTO;
import com.patta.api.dto.CapacidadeHospedagemResponseDTO;
import com.patta.api.model.ClinicaConfigModel;
import com.patta.api.repository.ClinicaConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ClinicaService {

    public static final String MENSAGEM_CLINICA_FECHADA =
            "A clínica está fechada no momento. Por favor, aguarde.";

    private final ClinicaConfigRepository clinicaConfigRepository;
    private final PapelUsuarioService papelUsuarioService;

    public ClinicaStatusDTO status() throws ExecutionException, InterruptedException {
        return new ClinicaStatusDTO(clinicaConfigRepository.buscar().getAberta());
    }

    public ClinicaStatusDTO fechar(String uid) throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        return alterarEstado(uid, false, "A clínica já está fechada.");
    }

    public ClinicaStatusDTO abrir(String uid) throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        return alterarEstado(uid, true, "A clínica já está aberta.");
    }

    public CapacidadeHospedagemResponseDTO capacidadeHospedagem(String uid)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        return new CapacidadeHospedagemResponseDTO(consultarCapacidadeHospedagem());
    }

    public Integer consultarCapacidadeHospedagem() throws ExecutionException, InterruptedException {
        return clinicaConfigRepository.buscar().getCapacidadeHospedagem();
    }

    public CapacidadeHospedagemResponseDTO configurarCapacidadeHospedagem(String uid, Integer capacidadeMaxima)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        if (capacidadeMaxima == null || capacidadeMaxima <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A capacidade máxima deve ser maior que zero.");
        }
        ClinicaConfigModel config = clinicaConfigRepository.atualizarCapacidadeHospedagem(capacidadeMaxima);
        return new CapacidadeHospedagemResponseDTO(config.getCapacidadeHospedagem());
    }

    public void exigirAbertaParaCliente(String uid) throws ExecutionException, InterruptedException {
        if (papelUsuarioService.podeGerenciarAtendimento(uid)) {
            return;
        }
        if (!clinicaConfigRepository.buscar().getAberta()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MENSAGEM_CLINICA_FECHADA);
        }
    }

    private ClinicaStatusDTO alterarEstado(String uid, boolean aberta, String mensagemConflito)
            throws ExecutionException, InterruptedException {
        try {
            ClinicaConfigModel config = clinicaConfigRepository.alterarEstado(uid, aberta);
            return new ClinicaStatusDTO(config.getAberta());
        } catch (ClinicaConfigRepository.EstadoClinicaInalteradoException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagemConflito, exception);
        }
    }

    private void exigirEquipe(String uid) {
        if (!papelUsuarioService.podeGerenciarAtendimento(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente profissionais ou administradores autorizados podem alterar o estado da clínica.");
        }
    }
}
