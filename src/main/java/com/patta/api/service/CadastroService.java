package com.patta.api.service;

import com.patta.api.dto.CadastroRequestDTO;
import com.patta.api.model.User;
import com.patta.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class CadastroService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // Cache temporário em memória: Armazena o Email como Chave, e os dados + código como Valor
    private final Map<String, DadosTemporarios> cacheCadastros = new ConcurrentHashMap<>();

    // 1. INICIA O FLUXO: Valida senhas, gera código, guarda na memória e envia e-mail
    public void solicitarCadastro(CadastroRequestDTO request) {
        if (!request.getSenha().equals(request.getConfirmaSenha())) {
            throw new RuntimeException("As senhas não coincidem!");
        }

        // Gera um código numérico de 6 dígitos
        String codigo = String.format("%06d", new Random().nextInt(999999));

        // Guarda os dados na memória (expira em 15 minutos)
        DadosTemporarios temp = new DadosTemporarios(request, codigo, LocalDateTime.now().plusMinutes(15));
        cacheCadastros.put(request.getEmail(), temp);

        // Envia o e-mail
        emailService.enviarCodigoVerificacao(request.getEmail(), codigo);
    }

    // 2. FINALIZA O FLUXO: Valida o código e salva no banco de dados (Firestore)
    public User confirmarCadastro(String email, String codigo) throws ExecutionException, InterruptedException {
        DadosTemporarios temp = cacheCadastros.get(email);

        if (temp == null) {
            throw new RuntimeException("Solicitação de cadastro não encontrada ou expirada.");
        }

        if (temp.expiracao.isBefore(LocalDateTime.now())) {
            cacheCadastros.remove(email);
            throw new RuntimeException("O código expirou. Solicite um novo cadastro.");
        }

        if (!temp.codigo.equals(codigo)) {
            throw new RuntimeException("Código inválido!");
        }

        // Código certo! Transforma o DTO no seu Model final e salva no Firestore
        User novoUser = User.builder()
                .nome(temp.dados.getNome())
                .email(temp.dados.getEmail())
                .telefone(temp.dados.getTelefone())
                // NOTA: Em um app real, criptografe a senha aqui (ex: BCrypt) antes de salvar
                .build();

        User salvo = userRepository.save(novoUser);

        // Remove da memória pois já foi salvo no banco
        cacheCadastros.remove(email);

        return salvo;
    }

    // Classe interna auxiliar para guardar os dados na memória
    private record DadosTemporarios(CadastroRequestDTO dados, String codigo, LocalDateTime expiracao) {}
}