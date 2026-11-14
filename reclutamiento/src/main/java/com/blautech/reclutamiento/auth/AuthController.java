package com.blautech.reclutamiento.auth;


import com.blautech.reclutamiento.auth.dto.LoginRequest;
import com.blautech.reclutamiento.auth.dto.LoginResponse;
import com.blautech.reclutamiento.auth.dto.RegistroRequest;
import com.blautech.reclutamiento.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @RestController = @Controller + @ResponseBody: todo lo que devuelvan
// estos métodos se convierte automáticamente a JSON.
// @RequestMapping("/auth") = todas las rutas de esta clase empiezan con
// /auth -- por eso en SecurityConfig marcamos "/auth/**" como público.
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST http://localhost:8080/auth/registro
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioResponse creado = authService.registrar(request);
        // 201 CREATED es el código correcto cuando se crea un recurso nuevo
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // POST http://localhost:8080/auth/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse respuesta = authService.login(request);
        return ResponseEntity.ok(respuesta);
    }
}
