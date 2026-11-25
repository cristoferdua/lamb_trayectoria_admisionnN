package pe.edu.upeu.lamb.admision.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lamb.admision.dto.PostulanteRequest;
import pe.edu.upeu.lamb.admision.dto.PostulanteResponse;
import org.springframework.beans.factory.annotation.Value;
import pe.edu.upeu.lamb.admision.service.PdfCarnetService;
import pe.edu.upeu.lamb.admision.service.PostulanteService;
import pe.edu.upeu.lamb.admision.service.QrService;

import java.util.List;

@Tag(name = "Postulante", description = "CRUD 2 - Postulante con datos del colegio y apoderado")
@RestController
@RequestMapping("/api/v1/postulantes")
@RequiredArgsConstructor
public class PostulanteController {
    private final PostulanteService service;
    private final QrService qrService;
    private final PdfCarnetService pdfService;

    @Value("${lamb.public-base-url:}")
    private String publicBaseUrl;

    @GetMapping
    public List<PostulanteResponse> listar(@RequestParam(required = false) Long idConvocatoria) {
        return service.listar(idConvocatoria);
    }

    @GetMapping("/{id}")
    public PostulanteResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostulanteResponse crear(@Valid @RequestBody PostulanteRequest r) { return service.crear(r); }

    @PutMapping("/{id}")
    public PostulanteResponse actualizar(@PathVariable Long id, @Valid @RequestBody PostulanteRequest r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }

    @GetMapping(value = "/{id}/carnet", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> carnet(@PathVariable Long id,
                                         @RequestParam(defaultValue = "320") int size) {
        PostulanteResponse p = service.obtener(id);
        // Si hay una URL pública configurada (ngrok/prod), el QR apunta al PDF:
        // al escanearlo desde el celular, el navegador descarga el carnet oficial.
        // Si no, cae al código plano (útil en dev sin túnel).
        String contenido = (publicBaseUrl == null || publicBaseUrl.isBlank())
            ? p.getCodigoPostulante()
            : publicBaseUrl.replaceAll("/$", "") + "/api/v1/postulantes/" + id + "/carnet.pdf";
        byte[] png = qrService.generar(contenido, size);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline()
            .filename("carnet-" + p.getCodigoPostulante() + ".png").build());
        return ResponseEntity.ok().headers(headers).body(png);
    }

    @GetMapping(value = "/{id}/carnet.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> carnetPdf(@PathVariable Long id) {
        PostulanteResponse p = service.obtener(id);
        byte[] pdf = pdfService.generar(p);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("carnet-" + p.getCodigoPostulante() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

}
