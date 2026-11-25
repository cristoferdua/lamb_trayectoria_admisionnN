package pe.edu.upeu.lamb.admision.adapter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lamb.admision.port.FinanzasPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador fake en memoria del puerto Finanzas. Reemplazable por un cliente
 * HTTP real cuando el ms de Finanzas del Estudiante este disponible, sin tocar
 * el dominio de Admision.
 */
@Slf4j
@Component
public class FinanzasInMemoryAdapter implements FinanzasPort {

    private final AtomicLong secuencia = new AtomicLong(1000);
    private final Map<Long, Cargo> cargos = new ConcurrentHashMap<>();

    @Override
    public Long crearCargoDerechoAdmision(Long idPostulante, BigDecimal monto, String periodo) {
        Long id = secuencia.incrementAndGet();
        Cargo c = new Cargo(id, idPostulante, periodo, monto, monto, Instant.now());
        cargos.put(id, c);
        log.info("component=finanzas-mock accion=crear-cargo id={} postulante={} monto={} periodo={}",
                id, idPostulante, monto, periodo);
        return id;
    }

    @Override
    public BigDecimal consultarSaldo(Long idCargo) {
        Cargo c = cargos.get(idCargo);
        if (c == null) return BigDecimal.valueOf(-1);
        return c.saldo;
    }

    @Override
    public boolean postulanteTieneDerechoPagado(Long idPostulante, String periodo) {
        return cargos.values().stream()
                .anyMatch(c -> c.idPostulante.equals(idPostulante)
                        && c.periodo.equals(periodo)
                        && c.saldo.compareTo(BigDecimal.ZERO) == 0);
    }

    /** Operacion solo para el adaptador fake: marca el cargo como pagado. */
    public void registrarPago(Long idCargo, BigDecimal monto) {
        Cargo c = cargos.get(idCargo);
        if (c == null) throw new IllegalArgumentException("Cargo no encontrado: " + idCargo);
        BigDecimal nuevo = c.saldo.subtract(monto);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0) nuevo = BigDecimal.ZERO;
        c.saldo = nuevo;
        log.info("component=finanzas-mock accion=registrar-pago cargo={} pagado={} saldo={}", idCargo, monto, nuevo);
    }

    public List<Cargo> listar() {
        return new ArrayList<>(cargos.values());
    }

    @Getter
    public static class Cargo {
        private final Long id;
        private final Long idPostulante;
        private final String periodo;
        private final BigDecimal monto;
        private BigDecimal saldo;
        private final Instant fechaCreacion;

        public Cargo(Long id, Long idPostulante, String periodo, BigDecimal monto, BigDecimal saldo, Instant fechaCreacion) {
            this.id = id;
            this.idPostulante = idPostulante;
            this.periodo = periodo;
            this.monto = monto;
            this.saldo = saldo;
            this.fechaCreacion = fechaCreacion;
        }
    }
}
