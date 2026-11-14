package com.blautech.reclutamiento.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Lo que manda el propio usuario logueado para cambiar SU contraseña
// (CU05). No es lo mismo que "reiniciar": el reinicio lo hace el
// Administrador sin saber la contraseña actual; aquí el usuario tiene
// que demostrar que conoce la vigente (temporal o no) antes de poder
// reemplazarla. Mismo mínimo de 8 caracteres que RegistroRequest, para
// no tener dos reglas distintas de "qué es una contraseña válida".
@Getter
@Setter
public class CambiarContrasenaRequest {

    @NotBlank(message = "Debes indicar tu contraseña actual")
    private String contrasenaActual;

    @NotBlank(message = "Debes indicar la nueva contraseña")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    private String contrasenaNueva;
}
