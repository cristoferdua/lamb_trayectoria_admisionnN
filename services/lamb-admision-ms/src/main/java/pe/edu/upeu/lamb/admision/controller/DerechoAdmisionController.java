package pe.edu.upeu.lamb.admision.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.adapter.FinanzasInMemoryAdapter;
import pe.edu.upeu.lamb.admision.port.FinanzasPort;
import pe.edu.upeu.lamb.admision.service.PagoReceivedListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Endpoint demo del adaptador fake de Finanzas. En produccion este flujo lo
 * expone el ms de Finanzas; aqui existe solo para probar el bloqueo del
 * examen cuando el derecho de admision no esta pagado.
 */
@RestController
@RequestMapping("/api/v1/derecho-admision")
@RequiredArgsConstructor
public class DerechoAdmisionController {

    private final FinanzasInMemoryAdapter adapter;
    private final FinanzasPort port;
    private final PagoReceivedListener pagoListener;

    @GetMapping
    public List<FinanzasInMemoryAdapter.Cargo> listar() {
        return adapter.listar();
    }

    @GetMapping("/{idCargo}/saldo")
    public Map<String, Object> saldo(@PathVariable Long idCargo) {
        BigDecimal saldo = port.consultarSaldo(idCargo);
        return Map.of("idCargo", idCargo, "saldo", saldo, "pagado", saldo.compareTo(BigDecimal.ZERO) == 0);
    }

    /** Admin confirma pago en caja (un solo click, sin formulario). */
    @PostMapping("/{idCargo}/confirmar-caja")
    public Map<String, Object> confirmarCaja(@PathVariable Long idCargo) {
        return registrarPagoCompleto(idCargo, "CAJA");
    }

    private Map<String, Object> registrarPagoCompleto(Long idCargo, String canal) {
        FinanzasInMemoryAdapter.Cargo c = adapter.listar().stream()
                .filter(x -> x.getId().equals(idCargo)).findFirst().orElse(null);
        if (c == null) return Map.of("error", "cargo no encontrado");
        adapter.registrarPago(idCargo, c.getMonto());
        pagoListener.onCargoPagado(c.getIdPostulante());
        return Map.of("idCargo", idCargo, "saldo", port.consultarSaldo(idCargo), "canal", canal, "pagado", true);
    }


    @GetMapping("/postulante/{idPostulante}")
    public Map<String, Object> estadoPostulante(@PathVariable Long idPostulante,
                                                @RequestParam String periodo) {
        boolean pagado = port.postulanteTieneDerechoPagado(idPostulante, periodo);
        return Map.of("idPostulante", idPostulante, "periodo", periodo, "pagado", pagado);
    }
}
