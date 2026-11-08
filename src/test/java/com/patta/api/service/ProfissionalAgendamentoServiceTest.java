package com.patta.api.service;

import com.patta.api.dto.AgendamentoResponseDTO;
import com.patta.api.dto.ProfissionalAgendamentoResponseDTO;
import com.patta.api.model.AnimalModel;
import com.patta.api.model.UserModel;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfissionalAgendamentoServiceTest {

    @Test
    void profissionalRecebeSomenteResumoDeAgendamentosDeUsuariosComuns() throws Exception {
        AgendamentoService agendamentoService = mock(AgendamentoService.class);
        AnimalRepository animalRepository = mock(AnimalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PapelUsuarioService papeis = new PapelUsuarioService("admin-uid", "vet-uid");
        ProfissionalAgendamentoService service = new ProfissionalAgendamentoService(
                agendamentoService, animalRepository, userRepository, papeis);

        when(agendamentoService.listarParaEquipe("vet-uid")).thenReturn(List.of(
                agendamento("booking-user", "user-uid", "pet-user", "consulta"),
                agendamento("booking-admin", "admin-uid", "pet-admin", "consulta"),
                agendamento("booking-vet", "vet-uid", "pet-vet", "hospedagem")));
        when(userRepository.findById("user-uid"))
                .thenReturn(Optional.of(UserModel.builder().id("user-uid").nome("Ana").build()));
        when(animalRepository.findById("pet-user"))
                .thenReturn(Optional.of(AnimalModel.builder().id("pet-user").nome("Rex").build()));

        List<ProfissionalAgendamentoResponseDTO> result = service.listar("vet-uid");

        assertEquals(1, result.size());
        assertEquals("Ana", result.getFirst().tutorNome());
        assertEquals("Rex", result.getFirst().petNome());
        assertEquals(LocalDate.of(2026, 11, 2), result.getFirst().data());
        assertEquals(LocalTime.of(10, 30), result.getFirst().horario());
        assertFalse(result.getFirst().hospedagem());
        assertTrue(result.getFirst().checkInDisponivel());
        assertEquals(List.of("id", "tutorNome", "petNome", "data", "horario", "dataSaidaPrevista",
                        "tempoHospedagemDias", "status", "permanenciaExcedente",
                        "permanenciaExcedenteDescricao", "hospedagem", "checkInDisponivel",
                        "checkOutDisponivel", "atendimentoDisponivel"),
                java.util.Arrays.stream(ProfissionalAgendamentoResponseDTO.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).findAll();
        org.mockito.Mockito.verify(animalRepository, org.mockito.Mockito.never()).findAll();
    }

    @Test
    void administradorRecebeResumoDeTodosOsPapeis() throws Exception {
        AgendamentoService agendamentoService = mock(AgendamentoService.class);
        AnimalRepository animalRepository = mock(AnimalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProfissionalAgendamentoService service = new ProfissionalAgendamentoService(
                agendamentoService, animalRepository, userRepository,
                new PapelUsuarioService("admin-uid", "vet-uid"));

        when(agendamentoService.listarParaEquipe("admin-uid")).thenReturn(List.of(
                agendamento("booking-vet", "vet-uid", "pet-vet", "hospedagem")));
        when(userRepository.findById("vet-uid"))
                .thenReturn(Optional.of(UserModel.builder().id("vet-uid").nome("Veterinário").build()));
        when(animalRepository.findById("pet-vet"))
                .thenReturn(Optional.of(AnimalModel.builder().id("pet-vet").nome("Pet Vet").build()));

        List<ProfissionalAgendamentoResponseDTO> result = service.listar("admin-uid");

        assertEquals(1, result.size());
        assertTrue(result.getFirst().hospedagem());
        assertEquals(3, result.getFirst().tempoHospedagemDias());
        assertEquals("APROVADO", result.getFirst().status());
        assertEquals(LocalDateTime.of(2026, 11, 5, 10, 30),
                result.getFirst().dataSaidaPrevista());
    }

    private AgendamentoResponseDTO agendamento(String id, String usuarioId, String animalId, String servico) {
        return new AgendamentoResponseDTO(
                id, usuarioId, animalId, servico, LocalDate.of(2026, 11, 2), LocalTime.of(10, 30),
                "hospedagem".equals(servico) ? LocalDateTime.of(2026, 11, 5, 10, 30) : null,
                "hospedagem".equals(servico) ? 3 : null, false, null,
                30, "APROVADO", "texto privado", null, null, null, null,
                Instant.parse("2026-11-01T10:00:00Z"), Instant.parse("2026-11-01T10:00:00Z"));
    }
}
