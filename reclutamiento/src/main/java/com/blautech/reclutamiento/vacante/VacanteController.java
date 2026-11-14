package com.blautech.reclutamiento.vacante;

import com.blautech.reclutamiento.vacante.dto.AsignarResponsableRequest;
import com.blautech.reclutamiento.vacante.dto.ResponsableResponse;
import com.blautech.reclutamiento.vacante.dto.VacanteHabilidadRequest;
import com.blautech.reclutamiento.vacante.dto.VacanteHabilidadResponse;
import com.blautech.reclutamiento.vacante.dto.VacanteRequest;
import com.blautech.reclutamiento.vacante.dto.VacanteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vacantes")
@RequiredArgsConstructor
public class VacanteController {

    private final VacanteService vacanteService;

    // @PreAuthorize("hasRole('RRHH')") revisa el rol que JwtAuthFilter
    // puso en el token (recuerda: JwtAuthFilter arma la autoridad como
    // "ROLE_" + rol). Si el usuario logueado NO es RRHH, Spring Security
    // responde automáticamente 403 Forbidden ANTES de que el método
    // siquiera se ejecute -- ni tenemos que validarlo a mano aquí.
    @PostMapping
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<VacanteResponse> crear(@Valid @RequestBody VacanteRequest request,
                                                 Authentication authentication) {
        var creada = vacanteService.crear(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Público para cualquier usuario autenticado (candidato o RRHH):
    // "consultar vacantes disponibles" (RF05).
    @GetMapping
    public ResponseEntity<List<VacanteResponse>> listarActivas() {
        return ResponseEntity.ok(vacanteService.listarActivas());
    }

    // Solo RRHH necesita ver también las cerradas/canceladas.
    @GetMapping("/todas")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<List<VacanteResponse>> listarTodas() {
        return ResponseEntity.ok(vacanteService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VacanteResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(vacanteService.obtener(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<VacanteResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody VacanteRequest request) {
        return ResponseEntity.ok(vacanteService.actualizar(id, request));
    }

    @PutMapping("/{id}/cerrar")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<VacanteResponse> cerrar(@PathVariable Integer id) {
        return ResponseEntity.ok(vacanteService.cerrar(id));
    }

    // Publica una vacante que se creó como BORRADOR (pasa a ACTIVA).
    @PutMapping("/{id}/publicar")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<VacanteResponse> publicar(@PathVariable Integer id) {
        return ResponseEntity.ok(vacanteService.publicar(id));
    }

    // GET /vacantes/responsables -> usuarios RRHH activos entre los que se
    // puede elegir al asignar/reasignar el responsable de una vacante.
    @GetMapping("/responsables")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<List<ResponsableResponse>> listarResponsables() {
        return ResponseEntity.ok(vacanteService.listarResponsables());
    }

    // PUT /vacantes/{id}/responsable -> reasigna quién lleva el proceso de
    // esta vacante.
    @PutMapping("/{id}/responsable")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<VacanteResponse> asignarResponsable(@PathVariable Integer id,
                                                               @Valid @RequestBody AsignarResponsableRequest request) {
        return ResponseEntity.ok(vacanteService.asignarResponsable(id, request));
    }

    @PostMapping("/{id}/habilidades")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<VacanteHabilidadResponse> agregarHabilidad(@PathVariable Integer id,
                                                             @Valid @RequestBody VacanteHabilidadRequest request) {
        var creada = vacanteService.agregarHabilidadRequerida(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
