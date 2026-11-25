package pe.edu.upeu.lamb.admision.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.entity.ProgramaAcademico;
import pe.edu.upeu.lamb.admision.entity.Sede;
import pe.edu.upeu.lamb.admision.repository.ProgramaAcademicoRepository;
import pe.edu.upeu.lamb.admision.repository.SedeRepository;

import java.util.List;
import java.util.Map;

/**
 * Catalogo publico de sedes y carreras para el wizard. Datos stub locales
 * hasta que exista el ms Institucional; endpoints migrables sin romper UI.
 */
@RestController
@RequestMapping("/api/v1/public/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final SedeRepository sedeRepo;
    private final ProgramaAcademicoRepository programaRepo;

    @GetMapping("/sedes")
    public List<Map<String, Object>> sedes() {
        return sedeRepo.findByActivoTrueOrderByNombre().stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "codigo", s.getCodigo(), "nombre", s.getNombre()))
                .toList();
    }

    @GetMapping("/programas")
    public List<Map<String, Object>> programas(@RequestParam(required = false) Long idSede) {
        List<ProgramaAcademico> data = (idSede != null)
                ? programaRepo.findBySedeIdAndActivoTrueOrderByNombre(idSede)
                : programaRepo.findByActivoTrueOrderByNombre();
        return data.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "codigo", p.getCodigo(),
                "nombre", p.getNombre(),
                "facultad", p.getFacultad(),
                "idSede", p.getSede().getId()
        )).toList();
    }
}
