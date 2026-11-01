package com.patta.api.service;

import com.patta.api.dto.RegistroClinicoRequestDTO;
import com.patta.api.dto.RegistroClinicoResponseDTO;
import com.patta.api.dto.VacinaRequestDTO;
import com.patta.api.dto.VacinaResponseDTO;
import com.patta.api.model.AnimalModel;
import com.patta.api.model.RegistroClinicoModel;
import com.patta.api.model.VacinaModel;
import com.patta.api.repository.AnimalRepository;
import com.patta.api.repository.RegistroClinicoRepository;
import com.patta.api.repository.VacinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class RegistroAnimalService {

    private final AnimalRepository animalRepository;
    private final RegistroClinicoRepository registroRepository;
    private final VacinaRepository vacinaRepository;
    private final PapelUsuarioService papelUsuarioService;

    public List<RegistroClinicoResponseDTO> listarHistoricoClinico(String uid, String animalId)
            throws ExecutionException, InterruptedException {
        buscarAnimalAutorizado(uid, animalId);
        return registroRepository.findByAnimalId(animalId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RegistroClinicoResponseDTO registrarHistoricoClinico(String uid, String animalId,
                                                                 RegistroClinicoRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        AnimalModel animal = buscarAnimal(animalId);
        if (request.dataAtendimento().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data do atendimento não pode estar no futuro.");
        }
        Date agora = new Date();
        RegistroClinicoModel registro = RegistroClinicoModel.builder()
                .animalId(animalId)
                .usuarioId(animal.getUserId())
                .dataAtendimento(Date.from(request.dataAtendimento()))
                .profissional(request.profissional().trim())
                .queixa(request.queixa().trim())
                .diagnostico(request.diagnostico().trim())
                .tratamento(request.tratamento().trim())
                .observacoes(normalizar(request.observacoes()))
                .registradoPor(uid)
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();
        return toResponse(registroRepository.save(registro));
    }

    public List<VacinaResponseDTO> listarVacinas(String uid, String animalId)
            throws ExecutionException, InterruptedException {
        buscarAnimalAutorizado(uid, animalId);
        return vacinaRepository.findByAnimalId(animalId).stream()
                .map(this::toResponse)
                .toList();
    }

    public VacinaResponseDTO registrarVacina(String uid, String animalId, VacinaRequestDTO request)
            throws ExecutionException, InterruptedException {
        exigirEquipe(uid);
        AnimalModel animal = buscarAnimal(animalId);
        if (request.aplicadaEm().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data de aplicação não pode estar no futuro.");
        }
        if (request.proximaDoseEm() != null && request.proximaDoseEm().isBefore(request.aplicadaEm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A próxima dose não pode ser anterior à aplicação.");
        }
        Date agora = new Date();
        VacinaModel vacina = VacinaModel.builder()
                .animalId(animalId)
                .usuarioId(animal.getUserId())
                .nome(request.nome().trim())
                .aplicadaEm(dataUtc(request.aplicadaEm()))
                .proximaDoseEm(request.proximaDoseEm() == null ? null : dataUtc(request.proximaDoseEm()))
                .lote(normalizar(request.lote()))
                .profissional(request.profissional().trim())
                .observacoes(normalizar(request.observacoes()))
                .registradoPor(uid)
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();
        return toResponse(vacinaRepository.save(vacina));
    }

    private AnimalModel buscarAnimalAutorizado(String uid, String animalId)
            throws ExecutionException, InterruptedException {
        AnimalModel animal = buscarAnimal(animalId);
        if (!uid.equals(animal.getUserId()) && !papelUsuarioService.ehAdministrador(uid)) {
            throw naoEncontrado();
        }
        return animal;
    }

    private AnimalModel buscarAnimal(String animalId) throws ExecutionException, InterruptedException {
        return animalRepository.findById(animalId).orElseThrow(RegistroAnimalService::naoEncontrado);
    }

    private void exigirEquipe(String uid) {
        if (!papelUsuarioService.podeGerenciarAtendimento(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores ou profissionais autorizados podem registrar dados clínicos ou vacinais.");
        }
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Date dataUtc(LocalDate data) {
        return Date.from(data.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private RegistroClinicoResponseDTO toResponse(RegistroClinicoModel registro) {
        return new RegistroClinicoResponseDTO(
                registro.getId(), registro.getAnimalId(), registro.getDataAtendimento().toInstant(),
                registro.getProfissional(), registro.getQueixa(), registro.getDiagnostico(),
                registro.getTratamento(), registro.getObservacoes(),
                registro.getCriadoEm().toInstant(), registro.getAtualizadoEm().toInstant());
    }

    private VacinaResponseDTO toResponse(VacinaModel vacina) {
        return new VacinaResponseDTO(
                vacina.getId(), vacina.getAnimalId(), vacina.getNome(),
                vacina.getAplicadaEm().toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
                vacina.getProximaDoseEm() == null
                        ? null
                        : vacina.getProximaDoseEm().toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
                vacina.getLote(), vacina.getProfissional(), vacina.getObservacoes(),
                vacina.getCriadoEm().toInstant());
    }

    private static ResponseStatusException naoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal não encontrado.");
    }
}
