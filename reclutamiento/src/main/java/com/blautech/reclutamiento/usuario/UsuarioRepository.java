package com.blautech.reclutamiento.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Lo vamos a usar constantemente: para el login (buscar el usuario por
    // su correo) y para validar que no exista ya alguien registrado con
    // ese mismo correo antes de crear una cuenta nueva.
    Optional<Usuario> findByCorreo(String correo);

    // Útil para el registro: antes de guardar, preguntamos "existsByCorreo"
    // en vez de traer el usuario completo, es una consulta más liviana.
    boolean existsByCorreo(String correo);

    // Usuarios RRHH activos, para que se pueda elegir un "responsable" al
    // asignar una vacante (reutiliza el mismo campo id_usuario_creador que
    // ya existía -- no agrega ninguna columna nueva a la base de datos).
    List<Usuario> findByRol_NombreAndEstado(String nombreRol, String estado);
}
