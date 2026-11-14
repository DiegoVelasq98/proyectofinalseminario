package com.blautech.reclutamiento.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Lo que el Administrador manda para activar o desactivar el acceso
// de un usuario (CU05 -- Gestionar usuarios y accesos).
@Getter
@Setter
public class CambiarEstadoUsuarioRequest {

    @NotBlank(message = "Debes indicar el nuevo estado")
    private String estado; // ACTIVO / INACTIVO
}
