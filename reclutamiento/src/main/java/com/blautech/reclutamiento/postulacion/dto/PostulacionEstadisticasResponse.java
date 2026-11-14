package com.blautech.reclutamiento.postulacion.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

// Datos agregados para el dashboard de RRHH: cuántas postulaciones hay
// en cada estado (embudo de conversión) y, de las que ya llegaron a
// CONTRATADO, cuánto tardaron en promedio desde que se recibieron.
@Getter
@Setter
public class PostulacionEstadisticasResponse {
    private Map<String, Long> porEstado;
    private Double tiempoPromedioContratacionDias; // null si todavía no hay ninguna CONTRATADA
    private Integer totalContratados;

    public PostulacionEstadisticasResponse(Map<String, Long> porEstado, Double tiempoPromedioContratacionDias,
                                            Integer totalContratados) {
        this.porEstado = porEstado;
        this.tiempoPromedioContratacionDias = tiempoPromedioContratacionDias;
        this.totalContratados = totalContratados;
    }
}
