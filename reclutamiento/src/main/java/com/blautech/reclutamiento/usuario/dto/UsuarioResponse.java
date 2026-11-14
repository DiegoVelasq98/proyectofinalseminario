package com.blautech.reclutamiento.usuario.dto;

import com.blautech.reclutamiento.usuario.Usuario;
import lombok.Getter;

import java.time.LocalDateTime;

// Lo que devolvemos cuando alguien consulta los datos de un usuario
// (ej. ms-reclutamiento preguntando "¿quién es el usuario 5?").
// Fíjate que NO incluye el campo "contrasena" -- ese jamás debe salir
// de ms-usuarios hacia ningún lado, ni siquiera en una respuesta interna.
@Getter
public class UsuarioResponse {
    private final Integer idUsuario;
    private final String nombre;
    private final String apellido;
    private final String correo;
    private final String telefono;
    private final String rol;
    private final String estado;
    private final LocalDateTime fechaCreacion;

    public UsuarioResponse(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.nombre = usuario.getNombre();
        this.apellido = usuario.getApellido();
        this.correo = usuario.getCorreo();
        this.telefono = usuario.getTelefono();
        this.rol = usuario.getRol().getNombre();
        this.estado = usuario.getEstado();
        this.fechaCreacion = usuario.getFechaCreacion();
    }
}
