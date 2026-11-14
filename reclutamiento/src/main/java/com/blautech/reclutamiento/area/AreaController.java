package com.blautech.reclutamiento.area;

import com.blautech.reclutamiento.area.dto.AreaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// GET /areas -> Angular usa esto para llenar el <select> de "Área" al
// crear o editar una vacante, en vez de tener la lista quemada en el
// frontend. Cualquier usuario autenticado puede consultarlo (igual que
// /habilidades); solo RRHH puede crear vacantes que lo usen, y solo
// RRHH puede administrar el catálogo (crear/editar/desactivar un área).
@RestController
@RequestMapping("/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @GetMapping
    public ResponseEntity<List<Area>> listar() {
        return ResponseEntity.ok(areaService.listarActivas());
    }

    // RRHH necesita ver también las INACTIVAS para poder reactivarlas
    // desde la pantalla de administración de áreas.
    @GetMapping("/todas")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<List<Area>> listarTodas() {
        return ResponseEntity.ok(areaService.listarTodas());
    }

    @PostMapping
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<Area> crear(@Valid @RequestBody AreaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(areaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<Area> actualizar(@PathVariable Integer id, @Valid @RequestBody AreaRequest request) {
        return ResponseEntity.ok(areaService.actualizar(id, request));
    }

    // "Eliminar" un área es, en realidad, desactivarla (ver AreaService):
    // deja de ofrecerse para vacantes nuevas, pero las vacantes que ya
    // la usaban siguen intactas, sin romper ninguna referencia.
    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<Area> desactivar(@PathVariable Integer id) {
        return ResponseEntity.ok(areaService.desactivar(id));
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<Area> activar(@PathVariable Integer id) {
        return ResponseEntity.ok(areaService.activar(id));
    }
}
