package pe.edu.upeu.lamb.admision.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.adapter.FinanzasInMemoryAdapter;
import pe.edu.upeu.lamb.admision.port.PersonaPort;
import pe.edu.upeu.lamb.admision.repository.PostulanteRepository;
import pe.edu.upeu.lamb.admision.service.PagoReceivedListener;

import java.util.List;
import java.util.Map;

/**
 * Pasarela web mock (Yape / BCP / MercadoPago). Endpoint publico que simula
 * la confirmacion de un pago online. Devuelve los cargos pendientes por DNI
 * para que el postulante vea cuanto debe.
 */
@RestController
@RequestMapping("/api/v1/public/pagar")
@RequiredArgsConstructor
public class PagoWebPublicoController {

    private final FinanzasInMemoryAdapter adapter;
    private final PagoReceivedListener pagoListener;
    private final PostulanteRepository postulanteRepo;
    private final PersonaPort personaPort;

    /** Lista cargos pendientes del postulante con ese DNI. */
    @GetMapping("/pendientes/{dni}")
    public List<Map<String, Object>> pendientes(@PathVariable String dni) {
        var persona = personaPort.consultarPorDni(dni).orElse(null);
        if (persona == null) return List.of();
        var ids = postulanteRepo.findByIdPersona(persona.id()).stream().map(p -> p.getId()).toList();
        return adapter.listar().stream()
                .filter(c -> ids.contains(c.getIdPostulante())
                        && c.getSaldo().compareTo(java.math.BigDecimal.ZERO) > 0)
                .map(c -> Map.<String, Object>of(
                        "idCargo", c.getId(),
                        "idPostulante", c.getIdPostulante(),
                        "monto", c.getMonto(),
                        "saldo", c.getSaldo(),
                        "periodo", c.getPeriodo()))
                .toList();
    }

    /** Pasarela mock: simula pago confirmado desde Yape/BCP/MercadoPago. */
    @PostMapping("/{idCargo}")
    public Map<String, Object> pagar(@PathVariable Long idCargo,
                                     @RequestParam(defaultValue = "YAPE") String canal) {
        FinanzasInMemoryAdapter.Cargo c = adapter.listar().stream()
                .filter(x -> x.getId().equals(idCargo)).findFirst().orElse(null);
        if (c == null) return Map.of("error", "cargo no encontrado");
        adapter.registrarPago(idCargo, c.getMonto());
        pagoListener.onCargoPagado(c.getIdPostulante());
        return Map.of("idCargo", idCargo, "canal", canal, "pagado", true);
    }
}
