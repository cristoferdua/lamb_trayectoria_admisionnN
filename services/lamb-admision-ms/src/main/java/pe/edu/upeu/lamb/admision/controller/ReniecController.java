package pe.edu.upeu.lamb.admision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.dto.ReniecResponse;
import pe.edu.upeu.lamb.admision.service.ReniecService;

@Tag(name = "RENIEC", description = "Servicio externo - consulta DNI via api.apis.net.pe")
@RestController
@RequestMapping("/api/v1/admision/reniec")
@RequiredArgsConstructor
public class ReniecController {
    private final ReniecService service;

    @Operation(summary = "Consultar datos de una persona por DNI")
    @GetMapping("/{dni}")
    public ReniecResponse consultar(@PathVariable String dni) {
        return service.consultarDni(dni);
    }
}
