package com.blautech.reclutamiento.habilidad;

import com.blautech.reclutamiento.habilidad.dto.HabilidadRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Este catálogo lo va a usar tanto "candidato" (para elegir sus
// habilidades) como "vacante" (para elegir las habilidades requeridas).
// Por eso vive aparte, en su propio paquete, en vez de meterse dentro
// de candidato o de vacante.
@RestController
@RequestMapping("/habilidades")
@RequiredArgsConstructor
public class HabilidadController {

    private final HabilidadService habilidadService;

    // GET http://localhost:8080/habilidades
    // Angular usa esto para llenar un <select> o autocompletar al armar
    // el perfil del candidato o los requisitos de una vacante.
    @GetMapping
    public ResponseEntity<List<Habilidad>> listar() {
        return ResponseEntity.ok(habilidadService.listarTodas());
    }

    // POST http://localhost:8080/habilidades
    // A proposito NO lleva @PreAuthorize de un rol especifico: tanto el
    // candidato (en "Mi perfil", al escribir una habilidad que no esta
    // en el catalogo) como RRHH (en el formulario de vacante, al pedir un
    // requisito nuevo) llaman este mismo endpoint para agregarla de una
    // vez al catalogo compartido -- restringirlo a un solo rol rompería
    // alguno de los dos flujos reales. Cualquier usuario AUTENTICADO
    // puede usarlo (eso ya lo exige SecurityConfig con anyRequest().authenticated());
    // lo unico que no puede hacer un anonimo es llegar aqui.
    @PostMapping
    public ResponseEntity<Habilidad> crear(@Valid @RequestBody HabilidadRequest request) {
        Habilidad creada = habilidadService.crear(request.getNombre(), request.getDescripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}