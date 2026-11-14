package com.blautech.reclutamiento.candidato;

import com.blautech.reclutamiento.candidato.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Fíjate en el parámetro "Authentication authentication" que se repite
// en cada método: Spring Security lo inyecta automáticamente con los
// datos que JwtAuthFilter dejó en el SecurityContext al validar el
// token. authentication.getName() nos da el correo del usuario logueado
// -- así CADA candidato solo puede ver/editar SU PROPIO perfil, nunca
// el de otro, sin que el frontend tenga que mandar explícitamente
// "de qué candidato" se trata (sería inseguro confiar en eso).
@RestController
@RequestMapping("/candidatos")
@RequiredArgsConstructor
public class CandidatoController {

    private final CandidatoService candidatoService;

    @PostMapping("/perfil")
    public ResponseEntity<CandidatoPerfilResponse> crearPerfil(
            @Valid @RequestBody CandidatoPerfilRequest request, Authentication authentication) {
        var creado = candidatoService.crearPerfil(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/perfil")
    public ResponseEntity<CandidatoPerfilResponse> obtenerPerfil(Authentication authentication) {
        return ResponseEntity.ok(candidatoService.obtenerPerfil(authentication.getName()));
    }

    @PutMapping("/perfil")
    public ResponseEntity<CandidatoPerfilResponse> actualizarPerfil(
            @Valid @RequestBody CandidatoPerfilRequest request, Authentication authentication) {
        return ResponseEntity.ok(candidatoService.actualizarPerfil(authentication.getName(), request));
    }

    @PostMapping("/experiencia")
    public ResponseEntity<ExperienciaLaboral> agregarExperiencia(
            @Valid @RequestBody ExperienciaLaboralRequest request, Authentication authentication) {
        var creada = candidatoService.agregarExperiencia(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/educacion")
    public ResponseEntity<Educacion> agregarEducacion(
            @Valid @RequestBody EducacionRequest request, Authentication authentication) {
        var creada = candidatoService.agregarEducacion(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Corregir/eliminar un registro de experiencia o educación ya
    // guardado -- antes solo se podían crear, nunca editar ni borrar.
    @PutMapping("/experiencia/{id}")
    public ResponseEntity<ExperienciaLaboral> actualizarExperiencia(
            @PathVariable Integer id, @Valid @RequestBody ExperienciaLaboralRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(candidatoService.actualizarExperiencia(authentication.getName(), id, request));
    }

    @DeleteMapping("/experiencia/{id}")
    public ResponseEntity<Void> eliminarExperiencia(@PathVariable Integer id, Authentication authentication) {
        candidatoService.eliminarExperiencia(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/educacion/{id}")
    public ResponseEntity<Educacion> actualizarEducacion(
            @PathVariable Integer id, @Valid @RequestBody EducacionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(candidatoService.actualizarEducacion(authentication.getName(), id, request));
    }

    @DeleteMapping("/educacion/{id}")
    public ResponseEntity<Void> eliminarEducacion(@PathVariable Integer id, Authentication authentication) {
        candidatoService.eliminarEducacion(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/habilidades")
    public ResponseEntity<CandidatoHabilidadResponse> agregarHabilidad(
            @Valid @RequestBody CandidatoHabilidadRequest request, Authentication authentication) {
        var creada = candidatoService.agregarHabilidad(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @DeleteMapping("/habilidades/{id}")
    public ResponseEntity<Void> eliminarHabilidad(@PathVariable Integer id, Authentication authentication) {
        candidatoService.eliminarHabilidad(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    // Le pone/cambia el nivel a una habilidad que ya está en el perfil
    // (sobre todo las que detectó el currículum, que quedan sin nivel).
    @PutMapping("/habilidades/{id}")
    public ResponseEntity<CandidatoHabilidadResponse> actualizarNivelHabilidad(
            @PathVariable Integer id, @Valid @RequestBody ActualizarNivelHabilidadRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(candidatoService.actualizarNivelHabilidad(authentication.getName(), id, request));
    }

    // Subida real del archivo (multipart/form-data, campo "archivo").
    // Reemplaza el registro anterior que solo guardaba nombre + enlace:
    // ahora el archivo se guarda de verdad en el servidor y se lee su
    // texto para detectar, contra el catálogo real de habilidades,
    // cuáles menciona el documento.
    @PostMapping(value = "/curriculum", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CurriculumProcesadoResponse> agregarCurriculum(
            @RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        var resultado = candidatoService.registrarYProcesarCurriculum(authentication.getName(), archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    // El propio candidato descarga su currículum más reciente.
    @GetMapping("/curriculum/archivo")
    public ResponseEntity<byte[]> descargarMiCurriculum(Authentication authentication) {
        return respuestaArchivo(candidatoService.obtenerArchivoCurriculumPropio(authentication.getName()));
    }

    // ------------------------------------------------------------------
    // Endpoints para RRHH: ver el directorio completo de candidatos y el
    // detalle de uno en particular (mismo formato que "Mi perfil", pero
    // consultado por RRHH en vez de por el propio candidato). Se agregaron
    // para soportar la pantalla "Candidatos" del panel de RRHH.
    // ------------------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<List<CandidatoPerfilResponse>> listarTodos() {
        return ResponseEntity.ok(candidatoService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<CandidatoPerfilResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(candidatoService.obtenerPorId(id));
    }

    // RRHH descarga el currículum real de un candidato en particular
    // (esto reemplaza el botón "Ver currículum" que antes estaba
    // deshabilitado en el detalle del candidato -- ahora sí hay un
    // archivo real detrás).
    @GetMapping("/{id}/curriculum/archivo")
    @PreAuthorize("hasRole('RRHH')")
    public ResponseEntity<byte[]> descargarCurriculumDeCandidato(@PathVariable Integer id) {
        return respuestaArchivo(candidatoService.obtenerArchivoCurriculumDeCandidato(id));
    }

    private ResponseEntity<byte[]> respuestaArchivo(CandidatoService.ArchivoCurriculum archivo) {
        boolean esPdf = "pdf".equalsIgnoreCase(archivo.tipoArchivo());
        MediaType tipo = esPdf
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        String nombreEscapado = archivo.nombreArchivo().replace("\"", "");
        // Los PDF se devuelven "inline" para que RRHH los pueda ver embebidos
        // en el navegador sin forzar la descarga; los .docx siguen forzando
        // descarga porque el navegador no los puede renderizar directamente.
        String disposicion = (esPdf ? "inline" : "attachment") + "; filename=\"" + nombreEscapado + "\"";
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion)
                .body(archivo.contenido());
    }
}
