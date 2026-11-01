package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.patta.api.dto.UserUpdateDTO;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import com.patta.api.repository.RegistroClinicoRepository;
import com.patta.api.repository.VacinaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void removeAnimaisEAgendamentosAntesDeRemoverPerfilEAContaFirebase() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        AnimalRepository animalRepository = mock(AnimalRepository.class);
        AgendamentoRepository agendamentoRepository = mock(AgendamentoRepository.class);
        RegistroClinicoRepository registroClinicoRepository = mock(RegistroClinicoRepository.class);
        VacinaRepository vacinaRepository = mock(VacinaRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        LoginService loginService = mock(LoginService.class);
        EmailService emailService = mock(EmailService.class);
        UserService userService = new UserService(
                userRepository, animalRepository, agendamentoRepository, registroClinicoRepository,
                vacinaRepository, firebaseAuth, loginService, emailService, new PapelUsuarioService(""));
        when(userRepository.findById("uid")).thenReturn(Optional.of(
                UserModel.builder().id("uid").build()));

        userService.deletarMinhaConta("uid");

        InOrder ordem = inOrder(animalRepository, agendamentoRepository, registroClinicoRepository,
                vacinaRepository, userRepository, firebaseAuth);
        ordem.verify(animalRepository).deleteByUserId("uid");
        ordem.verify(agendamentoRepository).deleteByUsuarioId("uid");
        ordem.verify(registroClinicoRepository).deleteByUsuarioId("uid");
        ordem.verify(vacinaRepository).deleteByUsuarioId("uid");
        ordem.verify(userRepository).deleteById("uid");
        ordem.verify(firebaseAuth).deleteUser("uid");
    }

    @Test
    void falhaAoExcluirAnimaisImpedeRemoverPerfilEAConta() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        AnimalRepository animalRepository = mock(AnimalRepository.class);
        AgendamentoRepository agendamentoRepository = mock(AgendamentoRepository.class);
        RegistroClinicoRepository registroClinicoRepository = mock(RegistroClinicoRepository.class);
        VacinaRepository vacinaRepository = mock(VacinaRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        LoginService loginService = mock(LoginService.class);
        EmailService emailService = mock(EmailService.class);
        UserService userService = new UserService(
                userRepository, animalRepository, agendamentoRepository, registroClinicoRepository,
                vacinaRepository, firebaseAuth, loginService, emailService, new PapelUsuarioService(""));
        when(userRepository.findById("uid")).thenReturn(Optional.of(
                UserModel.builder().id("uid").build()));
        doThrow(new ExecutionException(new RuntimeException("Firestore indisponível")))
                .when(animalRepository).deleteByUserId("uid");

        assertThrows(ExecutionException.class, () -> userService.deletarMinhaConta("uid"));

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void exigeSenhaAtualParaAlterarEmail() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = criarServico(userRepository, mock(FirebaseAuth.class),
                mock(LoginService.class), mock(EmailService.class));
        when(userRepository.findById("uid")).thenReturn(Optional.of(
                UserModel.builder().id("uid").nome("Nome").email("antigo@example.com").telefone("11999999999").build()));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> userService.atualizarMeuPerfil("uid",
                        new UserUpdateDTO("Nome novo", "11988888888", "novo@example.com", null)));

        Assertions.assertEquals("Informe sua senha atual para alterar o e-mail.", erro.getReason());
    }

    @Test
    void alteraEmailNoFirebaseEFirestoreEEnviaVerificacao() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        LoginService loginService = mock(LoginService.class);
        EmailService emailService = mock(EmailService.class);
        UserService userService = criarServico(userRepository, firebaseAuth, loginService, emailService);
        UserModel usuario = UserModel.builder()
                .id("uid").nome("Nome").email("antigo@example.com").telefone("11999999999").build();
        UserRecord contaFirebase = mock(UserRecord.class);
        when(userRepository.findById("uid")).thenReturn(Optional.of(usuario));
        when(firebaseAuth.getUser("uid")).thenReturn(contaFirebase);
        when(contaFirebase.isEmailVerified()).thenReturn(true);
        when(firebaseAuth.generateEmailVerificationLink("novo@example.com")).thenReturn("https://verify.example");

        var resposta = userService.atualizarMeuPerfil("uid",
                new UserUpdateDTO("Nome novo", "11988888888", " Novo@Example.com ", "senha-segura"));

        Assertions.assertEquals("novo@example.com", resposta.email());
        Assertions.assertEquals("Nome novo", resposta.nome());
        Assertions.assertEquals("11988888888", resposta.telefone());
        verify(loginService).validarSenhaAtual("uid", "antigo@example.com", "senha-segura");
        verify(firebaseAuth).updateUser(any(UserRecord.UpdateRequest.class));
        verify(emailService).enviarLinkVerificacao("novo@example.com", "https://verify.example");
        verify(userRepository).save(usuario);
    }

    @Test
    void atualizaNomeETelefoneSemReautenticarQuandoEmailNaoMuda() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        LoginService loginService = mock(LoginService.class);
        UserService userService = criarServico(userRepository, firebaseAuth, loginService, mock(EmailService.class));
        UserModel usuario = UserModel.builder()
                .id("uid").nome("Nome").email("ana@example.com").telefone("11999999999").build();
        when(userRepository.findById("uid")).thenReturn(Optional.of(usuario));

        userService.atualizarMeuPerfil("uid",
                new UserUpdateDTO("Novo nome", "11988888888", "ANA@example.com", null));

        verifyNoInteractions(firebaseAuth, loginService);
        Assertions.assertEquals("Novo nome", usuario.getNome());
        Assertions.assertEquals("11988888888", usuario.getTelefone());
    }

    private UserService criarServico(UserRepository userRepository, FirebaseAuth firebaseAuth,
                                     LoginService loginService, EmailService emailService) {
        return new UserService(
                userRepository,
                mock(AnimalRepository.class),
                mock(AgendamentoRepository.class),
                mock(RegistroClinicoRepository.class),
                mock(VacinaRepository.class),
                firebaseAuth,
                loginService,
                emailService,
                new PapelUsuarioService(""));
    }
}
