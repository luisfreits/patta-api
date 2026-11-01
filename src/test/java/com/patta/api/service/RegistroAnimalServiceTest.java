package com.patta.api.service;

import com.patta.api.dto.VacinaRequestDTO;
import com.patta.api.dto.RegistroClinicoRequestDTO;
import com.patta.api.model.AnimalModel;
import com.patta.api.model.RegistroClinicoModel;
import com.patta.api.model.VacinaModel;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.RegistroClinicoRepository;
import com.patta.api.repository.VacinaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistroAnimalServiceTest {

    private static final String OWNER_UID = "owner";
    private static final String ADMIN_UID = "admin";
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final RegistroClinicoRepository registroRepository = mock(RegistroClinicoRepository.class);
    private final VacinaRepository vacinaRepository = mock(VacinaRepository.class);
    private final RegistroAnimalService service = new RegistroAnimalService(
            animalRepository, registroRepository, vacinaRepository, new PapelUsuarioService(ADMIN_UID));

    @Test
    void administradorRegistraVacinaERespostaRetornaDadosDaAplicacao() throws Exception {
        when(animalRepository.findById("animal-1")).thenReturn(Optional.of(
                AnimalModel.builder().id("animal-1").userId(OWNER_UID).build()));
        when(vacinaRepository.save(any(VacinaModel.class))).thenAnswer(invocation -> {
            VacinaModel vacina = invocation.getArgument(0);
            vacina.setId("vaccine-1");
            return vacina;
        });
        LocalDate aplicadaEm = LocalDate.now().minusDays(1);

        var response = service.registrarVacina(ADMIN_UID, "animal-1",
                new VacinaRequestDTO("V10", aplicadaEm, aplicadaEm.plusYears(1),
                        "Lote-1", "Dra. Vet", null));

        assertEquals("vaccine-1", response.id());
        assertEquals(aplicadaEm, response.aplicadaEm());
        assertEquals(aplicadaEm.plusYears(1), response.proximaDoseEm());
    }

    @Test
    void tutorNaoPodeCriarRegistroDeVacina() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.registrarVacina(OWNER_UID, "animal-1",
                        new VacinaRequestDTO("V10", LocalDate.now(), null, null, "Dra. Vet", null)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(animalRepository, vacinaRepository);
    }

    @Test
    void profissionalAutorizadoPodeRegistrarAtendimento() throws Exception {
        RegistroAnimalService serviceProfissional = new RegistroAnimalService(
                animalRepository, registroRepository, vacinaRepository, new PapelUsuarioService("", "vet-1"));
        when(animalRepository.findById("animal-1")).thenReturn(Optional.of(
                AnimalModel.builder().id("animal-1").userId(OWNER_UID).build()));
        when(registroRepository.save(any())).thenAnswer(invocation -> {
            RegistroClinicoModel registro = invocation.getArgument(0, RegistroClinicoModel.class);
            registro.setId("record-1");
            return registro;
        });

        var response = serviceProfissional.registrarHistoricoClinico("vet-1", "animal-1",
                new RegistroClinicoRequestDTO(
                        Instant.now().minusSeconds(60), "Dra. Vet",
                        "Coceira", "Dermatite", "Tratamento tópico", null));

        assertEquals("record-1", response.id());
    }

    @Test
    void tutorNaoConsultaRegistroDeAnimalAlheio() throws Exception {
        when(animalRepository.findById("animal-2")).thenReturn(Optional.of(
                AnimalModel.builder().id("animal-2").userId("another-owner").build()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.listarVacinas(OWNER_UID, "animal-2"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(vacinaRepository);
    }

    @Test
    void profissionalNaoConsultaProntuarioPelaRotaDeUsuario() throws Exception {
        RegistroAnimalService serviceProfissional = new RegistroAnimalService(
                animalRepository, registroRepository, vacinaRepository,
                new PapelUsuarioService(ADMIN_UID, "vet-1"));
        when(animalRepository.findById("animal-2")).thenReturn(Optional.of(
                AnimalModel.builder().id("animal-2").userId(OWNER_UID).build()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceProfissional.listarVacinas("vet-1", "animal-2"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(vacinaRepository);
    }
}
