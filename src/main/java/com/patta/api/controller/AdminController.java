package com.patta.api.controller;

import com.patta.api.dto.AdminAnimalResponseDTO;
import com.patta.api.dto.UserResponseDTO;
import com.patta.api.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/usuarios")
    public List<UserResponseDTO> listarUsuarios(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return adminService.listarUsuarios(uid);
    }

    @GetMapping("/animais")
    public List<AdminAnimalResponseDTO> listarAnimais(@AuthenticationPrincipal String uid)
            throws ExecutionException, InterruptedException {
        return adminService.listarAnimais(uid);
    }
}
