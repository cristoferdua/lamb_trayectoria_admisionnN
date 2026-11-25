package pe.edu.upeu.lamb.admision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.dto.PostulacionWizardRequest;
import pe.edu.upeu.lamb.admision.dto.PostulacionWizardResponse;
import pe.edu.upeu.lamb.admision.service.PostulacionWizardService;

@Tag(name = "Wizard Admisión", description = "Flujo transaccional de nueva postulación (DNI + RENIEC + Postulante + evento Kafka)")
@RestController
@RequestMapping("/api/v1/admision/postulacion-wizard")
@RequiredArgsConstructor
public class PostulacionWizardController {

    private final PostulacionWizardService service;

    @Operation(summary = "Registrar postulación completa en una sola transacción")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostulacionWizardResponse registrar(@Valid @RequestBody PostulacionWizardRequest r) {
        return service.registrar(r);
    }
}
