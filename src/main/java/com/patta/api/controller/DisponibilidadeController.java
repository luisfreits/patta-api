package com.patta.api.controller;

import com.patta.api.dto.DisponibilidadeResponseDTO;
import com.patta.api.dto.HorarioFuncionamentoRequestDTO;
import com.patta.api.model.HorarioFuncionamentoModel;
import com.patta.api.service.DisponibilidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidadeService;

    @GetMapping("/disponibilidade")
    public DisponibilidadeResponseDTO consultar(
            @RequestParam String servico,
            @RequestParam LocalDate data,
            @RequestParam(required = false) Integer tempoHospedagemDias)
            throws ExecutionException, InterruptedException {
        return disponibilidadeService.consultar(servico, data, tempoHospedagemDias);
    }

    @GetMapping("/profissional/disponibilidade/padrao")
    public HorarioFuncionamentoModel consultarPadrao(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return disponibilidadeService.consultarPadrao(uid);
    }

    @PutMapping("/profissional/disponibilidade/padrao")
    @ResponseStatus(HttpStatus.OK)
    public HorarioFuncionamentoModel configurarPadrao(
            @AuthenticationPrincipal String uid,
            @Valid @RequestBody HorarioFuncionamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        return disponibilidadeService.configurarPadrao(uid, request);
    }

    @DeleteMapping("/profissional/disponibilidade/padrao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerPadrao(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        disponibilidadeService.removerPadrao(uid);
    }

    @PutMapping("/admin/disponibilidade/{servico}/{diaSemana}")
    @ResponseStatus(HttpStatus.OK)
    public HorarioFuncionamentoModel configurar(@AuthenticationPrincipal String uid,
                                                @PathVariable String servico,
                                                @PathVariable String diaSemana,
                                                @Valid @RequestBody HorarioFuncionamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        return disponibilidadeService.configurar(uid, servico, diaSemana, request);
    }

    @PutMapping("/admin/disponibilidade/{servico}/periodo")
    @ResponseStatus(HttpStatus.OK)
    public void configurarPeriodo(@AuthenticationPrincipal String uid,
                                  @PathVariable String servico,
                                  @RequestParam LocalDate inicio,
                                  @RequestParam LocalDate fim,
                                  @Valid @RequestBody HorarioFuncionamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        disponibilidadeService.configurarPeriodo(uid, servico, inicio, fim, request);
    }

    @DeleteMapping("/admin/disponibilidade/{servico}/{diaSemana}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal String uid,
                        @PathVariable String servico,
                        @PathVariable String diaSemana)
            throws ExecutionException, InterruptedException {
        disponibilidadeService.remover(uid, servico, diaSemana);
    }

    @DeleteMapping("/admin/disponibilidade/{servico}/periodo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerPeriodo(@AuthenticationPrincipal String uid,
                               @PathVariable String servico,
                               @RequestParam LocalDate inicio,
                               @RequestParam LocalDate fim)
            throws ExecutionException, InterruptedException {
        disponibilidadeService.removerPeriodo(uid, servico, inicio, fim);
    }
}
