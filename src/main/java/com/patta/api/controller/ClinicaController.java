package com.patta.api.controller;

import com.patta.api.dto.CapacidadeHospedagemRequestDTO;
import com.patta.api.dto.CapacidadeHospedagemResponseDTO;
import com.patta.api.dto.ClinicaStatusDTO;
import com.patta.api.service.ClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/profissional/clinica")
@RequiredArgsConstructor
public class ClinicaController {

    private final ClinicaService clinicaService;

    @GetMapping("/status")
    public ClinicaStatusDTO status() throws ExecutionException, InterruptedException {
        return clinicaService.status();
    }

    @PostMapping("/fechar")
    @ResponseStatus(HttpStatus.OK)
    public ClinicaStatusDTO fechar(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return clinicaService.fechar(uid);
    }

    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.OK)
    public ClinicaStatusDTO abrir(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return clinicaService.abrir(uid);
    }

    @GetMapping("/capacidade-hospedagem")
    public CapacidadeHospedagemResponseDTO capacidadeHospedagem(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return clinicaService.capacidadeHospedagem(uid);
    }

    @PutMapping("/capacidade-hospedagem")
    public CapacidadeHospedagemResponseDTO configurarCapacidadeHospedagem(
            @AuthenticationPrincipal String uid,
            @Valid @RequestBody CapacidadeHospedagemRequestDTO request)
            throws ExecutionException, InterruptedException {
        return clinicaService.configurarCapacidadeHospedagem(uid, request.capacidadeMaxima());
    }
}
