package com.blautech.reclutamiento.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Esto es lo que Angular nos va a mandar en el body cuando alguien
// intenta iniciar sesión: solo correo y contraseña, nada más.
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasena;
}
 