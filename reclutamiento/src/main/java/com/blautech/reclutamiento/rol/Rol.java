package com.blautech.reclutamiento.rol;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// @Entity le dice a Spring: "esta clase representa una tabla de la base de datos".
// @Table le dice CUÁL tabla exactamente (si no la pones, Spring asume que la
// tabla se llama igual que la clase, en este caso hubiera adivinado "rol" de
// todas formas, pero es buena práctica ser explícito).
@Entity
@Table(name = "rol")
@Getter      // Lombok genera automáticamente todos los getRol(), getNombre(), etc.
@Setter      // Lombok genera automáticamente todos los setRol(), setNombre(), etc.
@NoArgsConstructor // JPA (el motor que traduce Java <-> SQL) EXIGE un constructor vacío
public class Rol {

    @Id // marca cuál campo es la llave primaria (PK) de la tabla
    @GeneratedValue(strategy = GenerationType.IDENTITY) // el valor lo genera MySQL solo (AUTO_INCREMENT)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO"; // valor por defecto en Java, igual que en el script SQL

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
