package com.blautech.reclutamiento.auth.dto;

import lombok.Getter;

// Esto es lo que le devolvemos a Angular cuando el login sale bien.
// El "token" es lo más importante: Angular lo va a guardar y lo va a
// mandar en cada petición futura (en el header Authorization) para
// demostrar "ya inicié sesión, aquí está mi comprobante firmado".
@Getter
public class LoginResponse {
    private final String token;
    private final String tipo = "Bearer"; // así se acostumbra anunciar un JWT en el header
    private final Integer idUsuario;
    private final String nombre;
    private final String correo;
    private final String rol;
    // true cuando la contraseña con la que entró es una temporal que le
    // generó el Administrador (ver Usuario.debeCambiarContrasena):
    // Angular usa esto para mandarlo directo a /cambiar-contrasena antes
    // de dejarlo usar el resto del sistema.
    private final boolean debeCambiarContrasena;

    public LoginResponse(String token, Integer idUsuario, String nombre, String correo, String rol,
                          boolean debeCambiarContrasena) {
        this.token = token;
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.debeCambiarContrasena = debeCambiarContrasena;
    }
}
