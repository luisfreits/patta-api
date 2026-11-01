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
import org.springframework.web.bind.annotation.RequestMapping;
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
    public DisponibilidadeResponseDTO consultar(@RequestParam String servico, @RequestParam LocalDate data)
            throws ExecutionException, InterruptedException {
        return disponibilidadeService.consultar(servico, data);
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

    @DeleteMapping("/admin/disponibilidade/{servico}/{diaSemana}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal String uid,
                        @PathVariable String servico,
                        @PathVariable String diaSemana)
            throws ExecutionException, InterruptedException {
        disponibilidadeService.remover(uid, servico, diaSemana);
    }
}
