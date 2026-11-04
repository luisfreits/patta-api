package com.patta.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Dispara um e-mail simples de texto
    public void enviarCodigoVerificacao(String destinatario, String codigo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setSubject("Código de Verificação - Patta App");
        mensagem.setText("Olá!\n\nSeu código de verificação é: " + codigo + "\n\nEste código expira em 15 minutos.");

        mailSender.send(mensagem);
    }

    public void enviarLinkVerificacao(String destinatario, String link) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setSubject("Confirme seu novo e-mail - Patta");
        mensagem.setText("Olá!\n\nPara confirmar seu novo endereço de e-mail, acesse o link abaixo:\n\n"
                + link + "\n\nSe você não solicitou essa alteração, entre em contato com o suporte.");
        mailSender.send(mensagem);
    }

    public void enviarSolicitacaoContato(String destinatario, String nome, String email,
                                         String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setReplyTo(email);
        mensagem.setSubject("Contato pelo site - Tai Vet");
        mensagem.setText("Nova mensagem enviada pelo formulário Fale conosco.\n\n"
                + "Nome: " + nome + "\n"
                + "E-mail: " + email + "\n"
                + "Assunto: " + assunto + "\n\n"
                + "Mensagem:\n" + texto);
        mailSender.send(mensagem);
    }
}