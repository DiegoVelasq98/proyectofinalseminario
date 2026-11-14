package com.blautech.reclutamiento.rol;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<Rol, Integer> significa: "esto administra la entidad Rol,
// y su llave primaria es de tipo Integer".
// Con solo escribir "extends JpaRepository", Spring te regala GRATIS los
// métodos save(), findById(), findAll(), deleteById(), etc. Sin escribir
// una sola línea de SQL.
public interface RolRepository extends JpaRepository<Rol, Integer> {

    // Este método NO lo escribimos "a mano" en el sentido de implementarlo:
    // Spring Data JPA lee el NOMBRE del método y adivina la consulta SQL que
    // debe generar. "findByNombre" -> "SELECT * FROM rol WHERE nombre = ?"
    // Optional<Rol> significa "puede que no exista ese rol", y nos obliga a
    // manejar ese caso en el Service en vez de arriesgarnos a un error nulo.
    Optional<Rol> findByNombre(String nombre);
}
