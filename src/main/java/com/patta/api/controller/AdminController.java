package com.patta.api.controller;

import com.patta.api.dto.AdminAnimalResponseDTO;
import com.patta.api.dto.AdminAgendamentoRequestDTO;
import com.patta.api.dto.AdminUserResponseDTO;
import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.UserUpdateDTO;
import com.patta.api.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/usuarios")
    public List<AdminUserResponseDTO> listarUsuarios(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return adminService.listarUsuarios(uid);
    }

    @PutMapping("/usuarios/{usuarioUid}")
    public AdminUserResponseDTO atualizarUsuario(@AuthenticationPrincipal String uid,
                                                 @PathVariable String usuarioUid,
                                                 @Valid @RequestBody UserUpdateDTO request)
            throws ExecutionException, InterruptedException {
        return adminService.atualizarUsuario(uid, usuarioUid, request);
    }

    @DeleteMapping("/usuarios/{usuarioUid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarUsuario(@AuthenticationPrincipal String uid, @PathVariable String usuarioUid)
            throws ExecutionException, InterruptedException {
        adminService.deletarUsuario(uid, usuarioUid);
    }

    @GetMapping("/animais")
    public List<AdminAnimalResponseDTO> listarAnimais(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return adminService.listarAnimais(uid);
    }

    @GetMapping("/agendamentos")
    public List<AgendamentoResponseDTO> listarAgendamentos(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return adminService.listarAgendamentos(uid);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponseDTO criarAgendamento(
            @AuthenticationPrincipal String uid,
            @Valid @RequestBody AdminAgendamentoRequestDTO request)
            throws ExecutionException, InterruptedException {
        return adminService.criarAgendamento(uid, request);
    }

    @PostMapping("/agendamentos/{id}/check-in")
    public AgendamentoResponseDTO marcarCheckIn(@AuthenticationPrincipal String uid, @PathVariable String id)
            throws ExecutionException, InterruptedException {
        return adminService.marcarCheckIn(uid, id);
    }

    @PostMapping("/agendamentos/{id}/check-out")
    public AgendamentoResponseDTO marcarCheckOut(@AuthenticationPrincipal String uid, @PathVariable String id)
            throws ExecutionException, InterruptedException {
        return adminService.marcarCheckOut(uid, id);
    }
}
