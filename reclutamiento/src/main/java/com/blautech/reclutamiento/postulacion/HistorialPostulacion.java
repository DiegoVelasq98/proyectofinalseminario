package com.blautech.reclutamiento.postulacion;


import com.blautech.reclutamiento.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Bitácora: cada vez que cambia el estado de una postulación, queda un
// registro aquí -- qué estado tenía, a qué estado pasó, quién lo hizo
// y cuándo. Esto es lo que le da trazabilidad al proceso de selección
// (RF15 de tu tabla de requerimientos).
@Entity
@Table(name = "historial_postulacion")
@Getter
@Setter
@NoArgsConstructor
public class HistorialPostulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulacion", nullable = false)
    private Postulacion postulacion;

    // El usuario de RRHH que hizo el cambio (o el propio sistema en el
    // caso del registro inicial al postularse).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "estado_anterior", length = 30)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private String estadoNuevo;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();
}
