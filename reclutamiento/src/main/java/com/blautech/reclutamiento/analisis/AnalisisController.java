package com.blautech.reclutamiento.analisis;


import com.blautech.reclutamiento.analisis.dto.AnalisisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analisis")
@RequiredArgsConstructor
public class AnalisisController {

    private final AnalisisService analisisService;

    // POST /analisis/generar/5 -> genera (o regenera) el análisis de la
    // postulación 5. Por ahora lo dispara RRHH manualmente desde Angular
    // (ej. un botón "Analizar candidato" en la pantalla de postulantes);
    // más adelante se puede disparar automáticamente justo después de
    // que el candidato se postula, agregando una sola línea en
    // PostulacionService.
    @PostMapping("/generar/{idPostulacion}")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<AnalisisResponse> generar(@PathVariable Integer idPostulacion) {
        var resultado = analisisService.generarAnalisis(idPostulacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    // GET /analisis/postulacion/5 -> consultar el último análisis ya
    // generado (lo puede ver tanto RRHH como el propio candidato).
    @GetMapping("/postulacion/{idPostulacion}")
    public ResponseEntity<AnalisisResponse> obtener(@PathVariable Integer idPostulacion) {
        return ResponseEntity.ok(analisisService.obtenerUltimoPorPostulacion(idPostulacion));
    }
}
