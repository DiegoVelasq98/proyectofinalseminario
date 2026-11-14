package com.blautech.reclutamiento.area;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Catálogo de áreas/departamentos a los que puede pertenecer una vacante
// (Tecnología, Calidad, Infraestructura, Datos, etc.). Vive aparte, igual
// que Habilidad, para poder reutilizarse y crecer sin tocar Vacante.
@Entity
@Table(name = "area")
@Getter
@Setter
@NoArgsConstructor
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_area")
    private Integer idArea;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";
}
