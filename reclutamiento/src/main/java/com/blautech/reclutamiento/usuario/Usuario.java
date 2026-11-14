package com.blautech.reclutamiento.usuario;

import com.blautech.reclutamiento.rol.Rol;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    // @ManyToOne: "muchos usuarios pueden tener el mismo rol".
    // @JoinColumn le dice a JPA cuál columna de la tabla "usuario" guarda
    // la llave foránea hacia "rol" (en este caso id_rol). Como Rol vive en
    // la MISMA base de datos (ms_usuarios_db), aquí SÍ podemos usar una
    // relación JPA real con FK física, a diferencia de lo que pasará en
    // ms-reclutamiento cuando haga referencia a un usuario de este servicio.
    @ManyToOne(fetch = FetchType.LAZY) // LAZY = no traigas el Rol completo de la BD hasta que alguien lo pida explícitamente
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    // Aquí SIEMPRE va guardado el resultado de BCrypt (un hash), nunca la
    // contraseña real. Lo vemos en la Parte de seguridad/JWT.
    //
    // @JsonIgnore: HistorialPostulacion serializa su campo "usuario" como
    // esta misma entidad completa (en vez de un DTO) -- sin esto, el hash
    // de la contraseña de quien hizo el cambio de estado se filtraba tal
    // cual en la respuesta de GET /postulaciones/{id}/historial, visible
    // tanto para RRHH como para el candidato dueño de esa postulación.
    @JsonIgnore
    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasena;

    // Se pone en true cuando el Administrador reinicia la contraseña
    // desde "Usuario y Acceso" (ver UsuarioService.reiniciarContrasena):
    // la contraseña temporal que generamos ahí sirve para entrar, pero
    // el usuario debe reemplazarla por una propia antes de poder usar
    // el resto del sistema. Vuelve a false en cuanto cambia su contraseña
    // (ver UsuarioService.cambiarContrasenaPropia).
    @Column(name = "debe_cambiar_contrasena", nullable = false)
    private Boolean debeCambiarContrasena = false;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;
}