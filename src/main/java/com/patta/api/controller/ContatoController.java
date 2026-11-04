package com.patta.api.controller;

import com.patta.api.dto.ContatoRequestDTO;
import com.patta.api.dto.MessageResponseDTO;
import com.patta.api.service.ContatoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recebe solicitações públicas de contato e suporte. */
@RestController
@RequestMapping("/contato")
@RequiredArgsConstructor
public class ContatoController {

    private final ContatoService contatoService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponseDTO enviar(@Valid @RequestBody ContatoRequestDTO request) {
        contatoService.enviar(request);
        return new MessageResponseDTO("Mensagem enviada. Em breve entraremos em contato.");
    }
}
