package pe.edu.upeu.lamb.evaluacionn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lamb.evaluacionn.dto.ExamenRequest;
import pe.edu.upeu.lamb.evaluacionn.dto.ExamenResponse;
import pe.edu.upeu.lamb.evaluacionn.entity.Examen;
import pe.edu.upeu.lamb.evaluacionn.event.EvaluacionnEvento;
import pe.edu.upeu.lamb.evaluacionn.exception.ResourceNotFoundException;
import pe.edu.upeu.lamb.evaluacionn.mapper.ExamenMapper;
import pe.edu.upeu.lamb.evaluacionn.messaging.EvaluacionnEventosProducer;
import pe.edu.upeu.lamb.evaluacionn.repository.ExamenRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final ExamenMapper examenMapper;
    private final EvaluacionnEventosProducer producer;

    @Value("${spring.application.name}")
    private String nombreServicio;

    public List<ExamenResponse> listar() {
        return examenRepository.findAll().stream()
                .map(examenMapper::toResponse)
                .toList();
    }

    public ExamenResponse obtener(Long id) {
        return examenMapper.toResponse(buscarOFallar(id));
    }

    @Transactional
    public ExamenResponse crear(ExamenRequest request) {
        Examen examen = examenRepository.save(examenMapper.toEntity(request));
        publicar("examen.creado", examen);
        return examenMapper.toResponse(examen);
    }

    @Transactional
    public ExamenResponse actualizar(Long id, ExamenRequest request) {
        Examen examen = buscarOFallar(id);
        examen.setTitulo(request.getTitulo());
        examen.setCurso(request.getCurso());
        examen = examenRepository.save(examen);
        publicar("examen.actualizado", examen);
        return examenMapper.toResponse(examen);
    }

    @Transactional
    public void eliminar(Long id) {
        Examen examen = buscarOFallar(id);
        examenRepository.delete(examen);
        publicar("examen.eliminado", examen);
    }

    private Examen buscarOFallar(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado: " + id));
    }

    private void publicar(String tipoEvento, Examen examen) {
        producer.publicarTrasCommit(EvaluacionnEvento.builder()
                .tipoEvento(tipoEvento)
                .id(examen.getId())
                .mensaje(examen.getTitulo())
                .origen(nombreServicio)
                .timestamp(Instant.now().toEpochMilli())
                .build());
    }
}
