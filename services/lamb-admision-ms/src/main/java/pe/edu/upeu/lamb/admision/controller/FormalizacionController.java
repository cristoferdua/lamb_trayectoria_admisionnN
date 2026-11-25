package pe.edu.upeu.lamb.admision.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.service.FormalizacionService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/postulantes")
@RequiredArgsConstructor
public class FormalizacionController {

    private final FormalizacionService service;

    @PostMapping("/{id}/formalizar")
    public Map<String, Object> formalizar(@PathVariable Long id) {
        Long idCargo = service.formalizar(id);
        return Map.of("idPostulante", id, "idCargo", idCargo, "estado", "FORMALIZADO");
    }
}
