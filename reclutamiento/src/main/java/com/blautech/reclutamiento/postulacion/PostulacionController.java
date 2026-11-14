package com.blautech.reclutamiento.postulacion;

import com.blautech.reclutamiento.postulacion.dto.CambiarEstadoRequest;
import com.blautech.reclutamiento.postulacion.dto.PostulacionEstadisticasResponse;
import com.blautech.reclutamiento.postulacion.dto.PostulacionRequest;
import com.blautech.reclutamiento.postulacion.dto.PostulacionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/postulaciones")
@RequiredArgsConstructor
public class PostulacionController {

    private final PostulacionService postulacionService;

    // POST /postulaciones -> el candidato autenticado se postula
    @PostMapping
    public ResponseEntity<PostulacionResponse> postularse(@Valid @RequestBody PostulacionRequest request,
                                                          Authentication authentication) {
        var creada = postulacionService.postularse(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // GET /postulaciones/mias -> el candidato ve SUS propias postulaciones
    @GetMapping("/mias")
    public ResponseEntity<List<PostulacionResponse>> misPostulaciones(Authentication authentication) {
        return ResponseEntity.ok(postulacionService.misPostulaciones(authentication.getName()));
    }

    // GET /postulaciones/vacante/{id} -> RRHH ve los postulantes de una vacante
    @GetMapping("/vacante/{idVacante}")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<List<PostulacionResponse>> listarPorVacante(@PathVariable Integer idVacante) {
        return ResponseEntity.ok(postulacionService.listarPorVacante(idVacante));
    }

    // GET /postulaciones/candidato/{id} -> RRHH ve a qué vacantes se ha
    // postulado un candidato en particular (directorio de Candidatos).
    @GetMapping("/candidato/{idCandidato}")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<List<PostulacionResponse>> listarPorCandidato(@PathVariable Integer idCandidato) {
        return ResponseEntity.ok(postulacionService.listarPorCandidato(idCandidato));
    }

    // PUT /postulaciones/{id}/retirar -> el propio candidato retira SU
    // postulación (solo mientras sigue en RECIBIDA). Sin @PreAuthorize de
    // rol fijo porque la validación de "es tu postulación" depende del
    // dueño del dato, no del rol -- vive en el Service (mismo patrón que
    // obtenerHistorial/eliminarHabilidad).
    @PutMapping("/{id}/retirar")
    public ResponseEntity<PostulacionResponse> retirar(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(postulacionService.retirar(authentication.getName(), id));
    }

    // PUT /postulaciones/{id}/estado -> RRHH cambia el estado
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<PostulacionResponse> cambiarEstado(@PathVariable Integer id,
                                                             @Valid @RequestBody CambiarEstadoRequest request,
                                                             Authentication authentication) {
        var actualizada = postulacionService.cambiarEstado(authentication.getName(), id, request);
        return ResponseEntity.ok(actualizada);
    }

    // GET /postulaciones/estadisticas -> datos agregados para el dashboard
    // de RRHH (embudo por estado y tiempo promedio de contratación).
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<PostulacionEstadisticasResponse> obtenerEstadisticas() {
        return ResponseEntity.ok(postulacionService.obtenerEstadisticas());
    }

    // GET /postulaciones/{id}/historial -> ver la bitácora de cambios.
    // No lleva @PreAuthorize de rol fijo porque la pueden ver DOS tipos de
    // usuario distintos (RRHH, o el candidato dueño de la postulación) --
    // esa validación, al depender de A QUIÉN pertenece el dato y no solo
    // del rol, vive en el Service (mismo patrón que eliminarHabilidad).
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialPostulacion>> obtenerHistorial(@PathVariable Integer id,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(postulacionService.obtenerHistorial(authentication.getName(), id));
    }
}
