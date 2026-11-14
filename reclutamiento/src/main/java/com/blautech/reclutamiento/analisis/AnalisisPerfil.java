package com.blautech.reclutamiento.analisis;

import com.blautech.reclutamiento.postulacion.Postulacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analisis_perfil")
@Getter
@Setter
@NoArgsConstructor
public class AnalisisPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analisis")
    private Integer idAnalisis;

    // Ahora SÍ es una FK física real hacia Postulacion, porque ya
    // estamos en la misma base de datos (a diferencia de cuando este
    // módulo hubiera sido un microservicio aparte).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulacion", nullable = false)
    private Postulacion postulacion;

    @Column(name = "porcentaje_coincidencia", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeCoincidencia;

    @Column(name = "clasificacion", length = 30)
    private String clasificacion; // ALTA / MEDIA / BAJA

    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "fecha_analisis", nullable = false)
    private LocalDateTime fechaAnalisis = LocalDateTime.now();

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "COMPLETADO";
}
