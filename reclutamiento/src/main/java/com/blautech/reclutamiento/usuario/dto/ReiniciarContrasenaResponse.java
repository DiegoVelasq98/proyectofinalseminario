package com.blautech.reclutamiento.usuario.dto;

import lombok.Getter;

// Lo que el Administrador recibe justo después de reiniciar la contraseña
// de un usuario (CU05 -- Gestionar usuarios y accesos). Reemplaza al
// "¿Olvidaste tu contraseña?" que había en el login: como no hay envío de
// correo configurado en el proyecto (sin SMTP), el reinicio lo dispara el
// Administrador desde el panel "Usuario y Acceso" y esta es la ÚNICA vez
// que la contraseña nueva sale en texto plano del backend -- nunca se
// guarda así (se hashea con BCrypt antes de persistirse) ni se puede
// volver a consultar después de esta respuesta.
@Getter
public class ReiniciarContrasenaResponse {
    private final Integer idUsuario;
    private final String correo;
    private final String contrasenaTemporal;

    public ReiniciarContrasenaResponse(Integer idUsuario, String correo, String contrasenaTemporal) {
        this.idUsuario = idUsuario;
        this.correo = correo;
        this.contrasenaTemporal = contrasenaTemporal;
    }
}
