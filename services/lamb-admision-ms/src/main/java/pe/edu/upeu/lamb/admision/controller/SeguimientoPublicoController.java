package pe.edu.upeu.lamb.admision.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.dto.TimelineResponse;
import pe.edu.upeu.lamb.admision.service.TimelinePublicoService;

/**
 * Endpoint publico: el postulante consulta su estado por DNI. Sin login.
 */
@RestController
@RequestMapping("/api/v1/public/seguimiento")
@RequiredArgsConstructor
public class SeguimientoPublicoController {

    private final TimelinePublicoService service;

    @GetMapping("/{dni}")
    public TimelineResponse porDni(@PathVariable String dni) {
        return service.porDni(dni);
    }
}
