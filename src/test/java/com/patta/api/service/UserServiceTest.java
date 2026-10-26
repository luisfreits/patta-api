package com.patta.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AgendamentoRepository;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void removeAnimaisEAgendamentosAntesDeRemoverPerfilEAContaFirebase() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        AnimalRepository animalRepository = mock(AnimalRepository.class);
        AgendamentoRepository agendamentoRepository = mock(AgendamentoRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        UserService userService = new UserService(
                userRepository, animalRepository, agendamentoRepository, firebaseAuth);
        when(userRepository.findById("uid")).thenReturn(Optional.of(
                UserModel.builder().id("uid").build()));

        userService.deletarMinhaConta("uid");

        InOrder ordem = inOrder(animalRepository, agendamentoRepository, userRepository, firebaseAuth);
        ordem.verify(animalRepository).deleteByUserId("uid");
        ordem.verify(agendamentoRepository).deleteByUsuarioId("uid");
        ordem.verify(userRepository).deleteById("uid");
        ordem.verify(firebaseAuth).deleteUser("uid");
    }

    @Test
    void falhaAoExcluirAnimaisImpedeRemoverPerfilEAConta() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        AnimalRepository animalRepository = mock(AnimalRepository.class);
        AgendamentoRepository agendamentoRepository = mock(AgendamentoRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        UserService userService = new UserService(
                userRepository, animalRepository, agendamentoRepository, firebaseAuth);
        when(userRepository.findById("uid")).thenReturn(Optional.of(
                UserModel.builder().id("uid").build()));
        doThrow(new ExecutionException(new RuntimeException("Firestore indisponível")))
                .when(animalRepository).deleteByUserId("uid");

        assertThrows(ExecutionException.class, () -> userService.deletarMinhaConta("uid"));

        verifyNoInteractions(firebaseAuth);
    }
}
