package com.patta.api.controller;

import com.patta.api.dto.AgendamentoRequestDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.AgendamentoStatusRequestDTO;
import com.patta.api.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponseDTO criar(@AuthenticationPrincipal String uid,
                                        @Valid @RequestBody AgendamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        return agendamentoService.criar(uid, request);
    }

    @GetMapping
    public List<AgendamentoResponseDTO> listar(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return agendamentoService.listar(uid);
    }

    @GetMapping("/{id}")
    public AgendamentoResponseDTO buscar(@AuthenticationPrincipal String uid, @PathVariable String id)
            throws ExecutionException, InterruptedException {
        return agendamentoService.buscar(uid, id);
    }

    @PatchMapping("/{id}/status")
    public AgendamentoResponseDTO alterarStatus(@AuthenticationPrincipal String uid,
                                                @PathVariable String id,
                                                @Valid @RequestBody AgendamentoStatusRequestDTO request)
            throws ExecutionException, InterruptedException {
        return agendamentoService.alterarStatus(uid, id, request);
    }
}
