package pe.edu.upeu.lamb.admision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.lamb.admision.dto.ReniecResponse;

@Slf4j
@Service
public class ReniecService {

    private final RestClient restClient;
    private final String apiToken;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReniecService(
            @Value("${lamb.reniec.base-url:https://api.apis.net.pe}") String baseUrl,
            @Value("${lamb.reniec.token:}") String apiToken
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiToken = apiToken;
    }

    public ReniecResponse consultarDni(String dni) {
        if (dni == null || !dni.matches("\\d{8}")) {
            return ReniecResponse.builder()
                    .dni(dni)
                    .encontrado(false)
                    .fuente("validacion")
                    .build();
        }

        // Modo mock si no hay token configurado (demo sin acceso externo)
        if (apiToken == null || apiToken.isBlank()) {
            return mock(dni);
        }

        try {
            String raw = restClient.get()
                    .uri(uri -> uri.path("/v2/reniec/dni").queryParam("numero", dni).build())
                    .headers(h -> {
                        if (apiToken != null && !apiToken.isBlank()) {
                            h.setBearerAuth(apiToken);
                        }
                        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) return fallback(dni, "sin-respuesta");
            JsonNode body = mapper.readTree(raw);

            String nombres = text(body, "nombres");
            String paterno = text(body, "apellidoPaterno");
            String materno = text(body, "apellidoMaterno");
            if (nombres == null && paterno == null && materno == null) {
                return fallback(dni, "no-encontrado");
            }

            String completo = String.join(" ",
                    nz(nombres), nz(paterno), nz(materno)).trim();

            return ReniecResponse.builder()
                    .dni(dni)
                    .nombres(nombres)
                    .apellidoPaterno(paterno)
                    .apellidoMaterno(materno)
                    .nombreCompleto(completo)
                    .encontrado(true)
                    .fuente("apis.net.pe")
                    .build();
        } catch (Exception ex) {
            log.warn("RENIEC falló para DNI {}: {}", dni, ex.getMessage());
            return fallback(dni, "error:" + ex.getClass().getSimpleName());
        }
    }

    private ReniecResponse fallback(String dni, String motivo) {
        return ReniecResponse.builder().dni(dni).encontrado(false).fuente(motivo).build();
    }

    /** Datos ficticios deterministas por DNI para demo sin token. */
    private ReniecResponse mock(String dni) {
        String[] nombres = { "CRISTOFER DUA", "NELSON ANTONY", "MARIA JOSE", "JUAN CARLOS",
                             "ANA LUCIA", "LUIS FERNANDO", "SOFIA ISABEL", "DIEGO ALEJANDRO" };
        String[] paternos = { "ORDOÑEZ", "LARICO", "QUISPE", "MAMANI", "CHOQUE", "CONDORI",
                              "HUAMAN", "FLORES" };
        String[] maternos = { "QUISPE", "MAMANI", "CONDORI", "APAZA", "CHURA", "CALLATA",
                              "PACORI", "COAQUIRA" };
        int seed = Math.abs(dni.hashCode());
        String n = nombres[seed % nombres.length];
        String ap = paternos[(seed / 7) % paternos.length];
        String am = maternos[(seed / 13) % maternos.length];
        return ReniecResponse.builder()
                .dni(dni)
                .nombres(n)
                .apellidoPaterno(ap)
                .apellidoMaterno(am)
                .nombreCompleto(n + " " + ap + " " + am)
                .encontrado(true)
                .fuente("mock-demo")
                .build();
    }
    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
    private String nz(String s) { return s == null ? "" : s; }
}
