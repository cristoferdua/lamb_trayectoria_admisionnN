package pe.edu.upeu.lamb.admision.port;

import java.math.BigDecimal;

/**
 * Puerto de salida hacia el modulo Finanzas del Estudiante.
 *
 * Admision NO es dueno del dato de pagos: solo crea el cargo del derecho de
 * admision y consulta si fue saldado. El dueno real del cargo es el futuro
 * ms de Finanzas; hoy este puerto se implementa con un adaptador en memoria
 * y manana se cambia por un cliente HTTP sin tocar el dominio.
 */
public interface FinanzasPort {

    /** Crea el cargo de derecho de admision para un postulante. Devuelve el id de cargo. */
    Long crearCargoDerechoAdmision(Long idPostulante, BigDecimal monto, String periodo);

    /** Saldo pendiente del cargo. 0 = pagado. */
    BigDecimal consultarSaldo(Long idCargo);

    /** True si el postulante tiene el derecho de admision pagado (algun cargo con saldo 0). */
    boolean postulanteTieneDerechoPagado(Long idPostulante, String periodo);
}
