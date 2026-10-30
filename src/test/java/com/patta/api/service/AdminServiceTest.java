package com.patta.api.service;

import com.patta.api.model.AnimalModel;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private static final String ADMIN_UID = "admin-uid";
    private static final String USER_UID = "user-uid";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final AdminService service = new AdminService(
            userRepository, animalRepository, new PapelUsuarioService(ADMIN_UID));

    @Test
    void administradorPodeListarUsuarios() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(
                UserModel.builder().id(USER_UID).nome("Ana").email("ana@example.com").telefone("123").build()));

        var response = service.listarUsuarios(ADMIN_UID);

        assertEquals(USER_UID, response.getFirst().id());
        assertEquals("ana@example.com", response.getFirst().email());
        verify(userRepository).findAll();
    }

    @Test
    void administradorPodeListarAnimaisComUidDoDono() throws Exception {
        when(animalRepository.findAll()).thenReturn(List.of(
                AnimalModel.builder().id("animal-1").userId(USER_UID).nome("Rex")
                        .sexo("Macho").idade(3).animal("Cachorro").castrado(true).build()));

        var response = service.listarAnimais(ADMIN_UID);

        assertEquals(USER_UID, response.getFirst().userId());
        assertEquals("Rex", response.getFirst().nome());
        verify(animalRepository).findAll();
    }

    @Test
    void usuarioComumNaoPodeConsultarListagensAdministrativas() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.listarUsuarios(USER_UID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(userRepository, animalRepository);
    }
}
