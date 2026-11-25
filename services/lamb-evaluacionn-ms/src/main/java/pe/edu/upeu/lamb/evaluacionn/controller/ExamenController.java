package pe.edu.upeu.lamb.evaluacionn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.evaluacionn.dto.ExamenRequest;
import pe.edu.upeu.lamb.evaluacionn.dto.ExamenResponse;
import pe.edu.upeu.lamb.evaluacionn.service.ExamenService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/examenes")
@RequiredArgsConstructor
public class ExamenController {

    private final ExamenService examenService;

    @GetMapping
    public List<ExamenResponse> listar() {
        log.info("Listando examenes");
        return examenService.listar();
    }

    @GetMapping("/{id}")
    public ExamenResponse obtener(@PathVariable Long id) {
        return examenService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamenResponse crear(@Valid @RequestBody ExamenRequest request) {
        return examenService.crear(request);
    }

    @PutMapping("/{id}")
    public ExamenResponse actualizar(@PathVariable Long id, @Valid @RequestBody ExamenRequest request) {
        return examenService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        examenService.eliminar(id);
    }
}
