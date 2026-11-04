package com.patta.api.service;

import com.patta.api.dto.ContatoRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ContatoService {

    private final EmailService emailService;
    private final String destinatario;

    public ContatoService(EmailService emailService,
                          @Value("${app.contact.email}") String destinatario) {
        this.emailService = emailService;
        this.destinatario = destinatario;
    }

    public void enviar(ContatoRequestDTO request) {
        emailService.enviarSolicitacaoContato(
                destinatario,
                request.nome().trim(),
                request.email().trim(),
                request.assunto().trim(),
                request.mensagem().trim());
    }
}
