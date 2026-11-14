package com.blautech.reclutamiento.vacante;

import com.blautech.reclutamiento.habilidad.Habilidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Resuelve la relación N:M entre Vacante y Habilidad: "qué habilidades
// pide esta vacante". Este catálogo (junto con candidato_habilidad) es
// justo lo que el módulo de análisis va a comparar más adelante.
@Entity
@Table(name = "vacante_habilidad")
@Getter
@Setter
@NoArgsConstructor
public class VacanteHabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vacante_habilidad")
    private Integer idVacanteHabilidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vacante", nullable = false)
    private Vacante vacante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habilidad", nullable = false)
    private Habilidad habilidad;

    @Column(name = "nivel_requerido", length = 20)
    private String nivelRequerido;

    @Column(name = "es_obligatoria", nullable = false)
    private Boolean esObligatoria = true;
}
