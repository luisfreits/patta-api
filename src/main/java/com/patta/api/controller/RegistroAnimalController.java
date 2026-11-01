package com.patta.api.controller;

import com.patta.api.dto.RegistroClinicoRequestDTO;
import com.patta.api.dto.RegistroClinicoResponseDTO;
import com.patta.api.dto.VacinaRequestDTO;
import com.patta.api.dto.VacinaResponseDTO;
import com.patta.api.service.RegistroAnimalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
public class RegistroAnimalController {

    private final RegistroAnimalService registroAnimalService;

    @GetMapping("/animais/{animalId}/historico-clinico")
    public List<RegistroClinicoResponseDTO> listarHistoricoClinico(
            @AuthenticationPrincipal String uid, @PathVariable String animalId)
            throws ExecutionException, InterruptedException {
        return registroAnimalService.listarHistoricoClinico(uid, animalId);
    }

    @GetMapping("/admin/animais/{animalId}/historico-clinico")
    public List<RegistroClinicoResponseDTO> listarHistoricoClinicoAdmin(
            @AuthenticationPrincipal String uid, @PathVariable String animalId)
            throws ExecutionException, InterruptedException {
        return registroAnimalService.listarHistoricoClinico(uid, animalId);
    }

    @PostMapping("/admin/animais/{animalId}/historico-clinico")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroClinicoResponseDTO registrarHistoricoClinico(
            @AuthenticationPrincipal String uid,
            @PathVariable String animalId,
            @Valid @RequestBody RegistroClinicoRequestDTO request)
            throws ExecutionException, InterruptedException {
        return registroAnimalService.registrarHistoricoClinico(uid, animalId, request);
    }

    @GetMapping("/animais/{animalId}/vacinas")
    public List<VacinaResponseDTO> listarVacinas(
            @AuthenticationPrincipal String uid, @PathVariable String animalId)
            throws ExecutionException, InterruptedException {
        return registroAnimalService.listarVacinas(uid, animalId);
    }

    @GetMapping("/admin/animais/{animalId}/vacinas")
    public List<VacinaResponseDTO> listarVacinasAdmin(
            @AuthenticationPrincipal String uid, @PathVariable String animalId)
            throws ExecutionException, InterruptedException {
        return registroAnimalService.listarVacinas(uid, animalId);
    }

    @PostMapping("/admin/animais/{animalId}/vacinas")
    @ResponseStatus(HttpStatus.CREATED)
    public VacinaResponseDTO registrarVacina(
            @AuthenticationPrincipal String uid,
            @PathVariable String animalId,
            @Valid @RequestBody VacinaRequestDTO request)
            throws ExecutionException, InterruptedException {
        return registroAnimalService.registrarVacina(uid, animalId, request);
    }
}
