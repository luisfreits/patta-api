package com.patta.api.service;

import com.patta.api.dto.AnimalRequestDTO;
import com.patta.api.dto.AnimalResponseDTO;
import com.patta.api.model.AnimalModel;
import com.patta.api.repository.AnimalRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class AnimalServiceTest {

    private static final String UID = "owner-uid";
    private static final String OTHER_UID = "other-uid";
    private static final ValidatorFactory VALIDATION_FACTORY = Validation.buildDefaultValidatorFactory();

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final AnimalService animalService = new AnimalService(animalRepository);

    @BeforeEach
    void limparMocks() {
        org.mockito.Mockito.reset(animalRepository);
    }

    @AfterAll
    static void fecharValidationFactory() throws Exception {
        VALIDATION_FACTORY.close();
    }

    @Test
    void retornaListaVaziaQuandoUsuarioNaoTemAnimais() throws Exception {
        when(animalRepository.findByUserId(UID)).thenReturn(List.of());

        assertTrue(animalService.listar(UID, null, null).isEmpty());
        verify(animalRepository).findByUserId(UID);
    }

    @Test
    void criaVariosAnimaisComUidDoUsuarioAutenticado() throws Exception {
        when(animalRepository.save(any(AnimalModel.class))).thenAnswer(invocation -> {
            AnimalModel animal = invocation.getArgument(0);
            animal.setId("animal-" + animal.getNome());
            return animal;
        });

        AnimalResponseDTO primeiro = animalService.criar(UID, request("Rex", "Cachorro"));
        AnimalResponseDTO segundo = animalService.criar(UID, request("Mimi", "Gato"));

        assertEquals("animal-Rex", primeiro.id());
        assertEquals("animal-Mimi", segundo.id());
        ArgumentCaptor<AnimalModel> animais = ArgumentCaptor.forClass(AnimalModel.class);
        verify(animalRepository, times(2)).save(animais.capture());
        assertTrue(animais.getAllValues().stream().allMatch(animal -> UID.equals(animal.getUserId())));
    }

    @Test
    void listaSomenteOEscopoDoUidECombinaFiltros() throws Exception {
        when(animalRepository.findByUserId(UID)).thenReturn(List.of(
                animal("1", UID, "Rex Filhote", "Cachorro"),
                animal("2", UID, "Rex Adulto", "Gato"),
                animal("3", UID, "Mimi", "Cachorro"),
                animal("4", OTHER_UID, "Rex de outra conta", "Cachorro")));

        List<AnimalResponseDTO> encontrados = animalService.listar(UID, " rex ", "cachorro");

        assertEquals(1, encontrados.size());
        assertEquals("1", encontrados.getFirst().id());
        verify(animalRepository).findByUserId(UID);
    }

    @Test
    void filtrosEmBrancoSaoIgnorados() throws Exception {
        when(animalRepository.findByUserId(UID)).thenReturn(List.of(animal("1", UID, "Rex", "Cachorro")));

        assertEquals(1, animalService.listar(UID, " ", " ").size());
    }

    @Test
    void rejeitaFiltroMaiorQueLimite() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> animalService.listar(UID, "x".repeat(101), null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void consultaIndividualRetornaAnimalProprio() throws Exception {
        when(animalRepository.findById("animal-1")).thenReturn(
                Optional.of(animal("animal-1", UID, "Rex", "Cachorro")));

        assertEquals("Rex", animalService.buscar(UID, "animal-1").nome());
    }

    @Test
    void naoPermiteConsultarEditarOuExcluirAnimalDeOutroUsuario() throws Exception {
        when(animalRepository.findById("animal-de-outro")).thenReturn(
                Optional.of(animal("animal-de-outro", OTHER_UID, "Rex", "Cachorro")));

        ResponseStatusException leitura = assertThrows(ResponseStatusException.class,
                () -> animalService.buscar(UID, "animal-de-outro"));
        ResponseStatusException edicao = assertThrows(ResponseStatusException.class,
                () -> animalService.atualizar(UID, "animal-de-outro", request("Novo", "Gato")));
        ResponseStatusException exclusao = assertThrows(ResponseStatusException.class,
                () -> animalService.deletar(UID, "animal-de-outro"));

        assertEquals(HttpStatus.NOT_FOUND, leitura.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, edicao.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, exclusao.getStatusCode());
    }

    @Test
    void atualizaCamposEditaveisSemAlterarProprietario() throws Exception {
        AnimalModel proprio = animal("animal-1", UID, "Rex", "Cachorro");
        when(animalRepository.findById("animal-1")).thenReturn(Optional.of(proprio));
        when(animalRepository.save(any(AnimalModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnimalResponseDTO atualizado = animalService.atualizar(UID, "animal-1", request("Max", "Gato"));

        assertEquals("Max", atualizado.nome());
        assertEquals("Gato", atualizado.animal());
        assertEquals(UID, proprio.getUserId());
        assertEquals("animal-1", proprio.getId());
    }

    @Test
    void exclusaoDeletaSomenteDepoisDeConfirmarProprietario() throws Exception {
        when(animalRepository.findById("animal-1")).thenReturn(
                Optional.of(animal("animal-1", UID, "Rex", "Cachorro")));

        animalService.deletar(UID, "animal-1");

        verify(animalRepository).deleteById("animal-1");
    }

    @Test
    void dadosInvalidosSaoRejeitadosPeloDto() {
        AnimalRequestDTO request = new AnimalRequestDTO(" ", "Cachorro", -1, "Cachorro", null);

        assertFalse(VALIDATION_FACTORY.getValidator().validate(request).isEmpty());
    }

    @Test
    void propagaFalhasDoFirestoreSemRetornarSucesso() throws Exception {
        when(animalRepository.findByUserId(UID)).thenThrow(new ExecutionException(new RuntimeException("Firestore indisponível")));

        assertThrows(ExecutionException.class, () -> animalService.listar(UID, null, null));
    }

    private AnimalRequestDTO request(String nome, String especie) {
        return new AnimalRequestDTO(nome, "Fêmea", 3, especie, false);
    }

    private AnimalModel animal(String id, String userId, String nome, String especie) {
        return AnimalModel.builder()
                .id(id)
                .userId(userId)
                .nome(nome)
                .sexo("Macho")
                .idade(2)
                .animal(especie)
                .castrado(false)
                .build();
    }
}
