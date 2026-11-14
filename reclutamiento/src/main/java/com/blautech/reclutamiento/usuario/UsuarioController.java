package com.blautech.reclutamiento.usuario;

import com.blautech.reclutamiento.usuario.dto.CambiarEstadoUsuarioRequest;
import com.blautech.reclutamiento.usuario.dto.CambiarRolRequest;
import com.blautech.reclutamiento.usuario.dto.ReiniciarContrasenaResponse;
import com.blautech.reclutamiento.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Soporta CU05 -- Gestionar usuarios y accesos (agregado formalmente al
// Capítulo III junto con RF16, historias 1011/1012). Protegido con
// @PreAuthorize("hasRole('ADMIN')") igual que VacanteController protege
// sus rutas con hasRole('RRHH') -- mismo patrón ya usado en el proyecto,
// ningún mecanismo nuevo. "ADMIN" es el valor real sembrado en la tabla
// rol (ver INSERT INTO rol en blautech_bd.sql), no "ADMINISTRADOR".
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // GET /usuarios -> el Administrador consulta el listado de usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // PUT /usuarios/{id}/rol -> el Administrador asigna/actualiza el rol
    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Integer id,
                                                      @Valid @RequestBody CambiarRolRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request));
    }

    // PUT /usuarios/{id}/estado -> el Administrador activa/desactiva el acceso
    @PutMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable Integer id,
                                                         @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request));
    }

    // PUT /usuarios/{id}/contrasena/reiniciar -> el Administrador genera una
    // contraseña temporal nueva (reemplaza el "¿Olvidaste tu contraseña?"
    // que había en el login; no hay envío de correo configurado en el
    // proyecto, así que el reinicio lo hace el Administrador a mano).
    @PutMapping("/{id}/contrasena/reiniciar")
    public ResponseEntity<ReiniciarContrasenaResponse> reiniciarContrasena(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.reiniciarContrasena(id));
    }
}
