package com.blautech.reclutamiento.vacante;


import com.blautech.reclutamiento.area.Area;
import com.blautech.reclutamiento.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacante")
@Getter
@Setter
@NoArgsConstructor
public class Vacante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vacante")
    private Integer idVacante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    private Usuario usuarioCreador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area")
    private Area area;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "conocimientos_requeridos", columnDefinition = "TEXT")
    private String conocimientosRequeridos;

    @Column(name = "ubicacion", length = 150)
    private String ubicacion;

    @Column(name = "modalidad", length = 30)
    private String modalidad; // PRESENCIAL / REMOTO / HIBRIDO

    @Column(name = "experiencia_requerida", length = 100)
    private String experienciaRequerida;

    @Column(name = "nivel_academico", length = 50)
    private String nivelAcademico;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVA"; // BORRADOR / ACTIVA / CERRADA / CANCELADA
}
