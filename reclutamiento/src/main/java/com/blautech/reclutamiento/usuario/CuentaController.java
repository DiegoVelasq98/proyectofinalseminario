package com.blautech.reclutamiento.usuario;

import com.blautech.reclutamiento.usuario.dto.CambiarContrasenaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Autoservicio de contraseña: CUALQUIER usuario logueado (CANDIDATO,
// RRHH o ADMIN) puede cambiar SU PROPIA contraseña aquí -- a diferencia
// de UsuarioController, que es exclusivo del rol ADMIN para administrar
// a OTROS usuarios (por eso va en un controller separado, sin el
// @PreAuthorize("hasRole('ADMIN')") de la clase de al lado).
//
// Mismo patrón que ya usa CandidatoController: el parámetro
// "Authentication authentication" lo inyecta Spring Security con lo
// que JwtAuthFilter dejó en el SecurityContext, y authentication.getName()
// da el correo del usuario logueado -- así nadie puede cambiar la
// contraseña de otra persona mandando un id distinto.
@RestController
@RequestMapping("/mi-cuenta")
@RequiredArgsConstructor
public class CuentaController {

    private final UsuarioService usuarioService;

    // PUT /mi-cuenta/contrasena -> cambia la contraseña del usuario
    // logueado. Lo usa, sobre todo, quien entró con la contraseña
    // temporal que le generó el Administrador (ver debeCambiarContrasena).
    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest request,
                                                  Authentication authentication) {
        usuarioService.cambiarContrasenaPropia(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
