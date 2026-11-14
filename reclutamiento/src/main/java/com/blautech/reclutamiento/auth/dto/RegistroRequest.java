package com.blautech.reclutamiento.auth.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Lo que Angular manda cuando un candidato se registra por primera vez.
// Nota: aquí NO pedimos "rol" — cualquiera que se registre desde este
// formulario público entra automáticamente como CANDIDATO. Las cuentas
// de RRHH/ADMIN se crean aparte (por ejemplo, un administrador las da
// de alta), nunca desde el registro público. Esto es una decisión de
// seguridad: si dejáramos que cualquiera elija su propio rol al
// registrarse, cualquier persona podría registrarse como RRHH.
@Getter
@Setter
public class RegistroRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasena;

    private String telefono; // opcional, por eso no lleva @NotBlank
}
