package com.patta.api.service;

import com.patta.api.dto.ContatoRequestDTO;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContatoServiceTest {

    @Test
    void encaminhaSolicitacaoParaOEmailDeSuporteComDadosNormalizados() {
        EmailService emailService = mock(EmailService.class);
        ContatoService contatoService = new ContatoService(emailService, "suporte@taivet.com");
        ContatoRequestDTO request = new ContatoRequestDTO(
                "  Ana  ", " ana@example.com ", "  Dúvida  ", "  Preciso de ajuda.  ");

        contatoService.enviar(request);

        verify(emailService).enviarSolicitacaoContato(
                "suporte@taivet.com", "Ana", "ana@example.com", "Dúvida", "Preciso de ajuda.");
    }
}
