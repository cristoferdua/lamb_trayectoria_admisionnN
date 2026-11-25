package pe.edu.upeu.lamb.admision.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Envío de correos vía Brevo API (https://developers.brevo.com/reference/sendtransacemail).
 *
 * Si {@code lamb.brevo.key} está vacío → modo MOCK (solo loguea). Útil para dev sin cuenta.
 */
@Slf4j
@Service
public class EmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public EmailService(
            @Value("${lamb.brevo.base-url:https://api.brevo.com}") String baseUrl,
            @Value("${lamb.brevo.key:}") String apiKey,
            @Value("${lamb.brevo.from-email:}") String fromEmail,
            @Value("${lamb.brevo.from-name:Admisión UPeU}") String fromName
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    /**
     * Envía correo HTML. Devuelve true si Brevo aceptó (201) o si estamos en modo mock.
     * Nunca lanza excepción: fallo de correo nunca tumba la transacción de postulación.
     */
    public boolean enviarHtml(String destinatarioEmail, String destinatarioNombre, String asunto, String htmlBody) {
        if (destinatarioEmail == null || destinatarioEmail.isBlank()) {
            log.warn("email=skip motivo=sin-destinatario asunto=\"{}\"", asunto);
            return false;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.info("email=mock to={} nombre={} asunto=\"{}\" status=simulado",
                    destinatarioEmail, destinatarioNombre, asunto);
            return true;
        }

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of(
                        "email", destinatarioEmail,
                        "name", destinatarioNombre == null ? destinatarioEmail : destinatarioNombre
                )),
                "subject", asunto,
                "htmlContent", htmlBody
        );

        try {
            var response = restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            boolean ok = response.getStatusCode() == HttpStatus.CREATED
                      || response.getStatusCode() == HttpStatus.ACCEPTED
                      || response.getStatusCode() == HttpStatus.OK;
            log.info("email=send to={} asunto=\"{}\" status={} code={}",
                    destinatarioEmail, asunto, ok ? "ok" : "error", response.getStatusCode().value());
            return ok;
        } catch (Exception ex) {
            log.warn("email=error to={} asunto=\"{}\" error=\"{}\"",
                    destinatarioEmail, asunto, ex.getMessage());
            return false;
        }
    }

    public String plantillaBienvenidaPostulante(String nombreCompleto, String codigoPostulante, String periodo) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><title>Bienvenido a UPeU</title></head>
            <body style="margin:0;padding:0;background:#F6F7FB;font-family:Arial,sans-serif;color:#1A2456">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F6F7FB;padding:40px 20px">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)">
                    <tr>
                      <td style="background:#1A2456;padding:28px 32px;color:#fff">
                        <div style="display:inline-block;width:40px;height:40px;background:#F5B800;color:#1A2456;border-radius:50%%;text-align:center;line-height:40px;font-weight:800;font-size:18px">A</div>
                        <span style="font-weight:700;font-size:16px;margin-left:10px;vertical-align:top;line-height:40px">UPeU · Admisión</span>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px">
                        <h1 style="margin:0 0 12px;font-size:24px;color:#1A2456">¡Bienvenido/a, %s!</h1>
                        <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#475569">
                          Tu postulación ha sido <strong>registrada correctamente</strong> en el sistema de Admisión UPeU
                          para el periodo <strong>%s</strong>. Guarda este correo con tu código de postulante.
                        </p>
                        <div style="background:#F6F7FB;border-left:4px solid #F5B800;padding:20px;border-radius:8px;margin:24px 0">
                          <div style="font-size:11px;font-weight:700;letter-spacing:.5px;color:#6B7280;text-transform:uppercase;margin-bottom:6px">Código de postulante</div>
                          <div style="font-size:28px;font-weight:800;color:#1A2456;letter-spacing:2px">%s</div>
                        </div>
                        <h3 style="margin:24px 0 10px;font-size:16px;color:#1A2456">Próximos pasos</h3>
                        <ul style="margin:0 0 20px;padding-left:20px;font-size:14px;line-height:1.7;color:#475569">
                          <li>Revisa el calendario de la convocatoria en el portal.</li>
                          <li>Prepara los documentos físicos y sube los digitales al portal del postulante.</li>
                          <li>El día del examen presenta tu DNI y este código.</li>
                        </ul>
                        <p style="margin:24px 0 0;font-size:13px;color:#6B7280">
                          Si tienes alguna consulta, contáctanos al correo de admisión o acércate a Filial Juliaca.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#F6F7FB;padding:18px 32px;text-align:center;font-size:12px;color:#6B7280;border-top:1px solid #E5E7EB">
                        <strong style="color:#1A2456">Universidad Peruana Unión</strong> · Admisión 2026-2<br/>
                        Este correo fue generado automáticamente. No responder a esta dirección.
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(nombreCompleto, periodo, codigoPostulante);
    }
}
