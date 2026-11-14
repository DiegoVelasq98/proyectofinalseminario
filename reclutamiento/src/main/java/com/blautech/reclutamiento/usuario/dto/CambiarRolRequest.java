package com.blautech.reclutamiento.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Lo que el Administrador manda para cambiar el rol de un usuario
// (CU05 -- Gestionar usuarios y accesos, agregado al Capítulo III).
@Getter
@Setter
public class CambiarRolRequest {

    @NotBlank(message = "Debes indicar el nuevo rol")
    private String nombreRol; // CANDIDATO / RRHH / ADMIN
}
