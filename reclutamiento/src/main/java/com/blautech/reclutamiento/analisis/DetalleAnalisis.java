package com.blautech.reclutamiento.analisis;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// El desglose criterio por criterio: por cada habilidad que pedía la
// vacante, qué tanto la cumple el candidato. Esto es lo que le permite
// a RRHH entender el "por qué" detrás del porcentaje general (RF14).
@Entity
@Table(name = "detalle_analisis")
@Getter
@Setter
@NoArgsConstructor
public class DetalleAnalisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_analisis", nullable = false)
    private AnalisisPerfil analisisPerfil;

    @Column(name = "criterio", nullable = false, length = 100)
    private String criterio; // ej. nombre de la habilidad evaluada

    @Column(name = "valor_esperado", length = 255)
    private String valorEsperado; // lo que pedía la vacante

    @Column(name = "valor_candidato", length = 255)
    private String valorCandidato; // lo que realmente tiene el candidato

    @Column(name = "porcentaje_coincidencia", precision = 5, scale = 2)
    private BigDecimal porcentajeCoincidencia;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
}
