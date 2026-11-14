package com.blautech.reclutamiento.analisis.dto;


import com.blautech.reclutamiento.analisis.AnalisisPerfil;
import com.blautech.reclutamiento.analisis.DetalleAnalisis;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AnalisisResponse {
    private Integer idAnalisis;
    private Integer idPostulacion;
    private BigDecimal porcentajeCoincidencia;
    private String clasificacion;
    private String resumen;
    private LocalDateTime fechaAnalisis;
    private List<DetalleAnalisis> detalles;
    private List<HabilidadExtra> habilidadesExtra;

    public AnalisisResponse(AnalisisPerfil analisis, List<DetalleAnalisis> detalles) {
        this(analisis, detalles, List.of());
    }

    // habilidadesExtra: habilidades del candidato que la vacante no pidió --
    // ver HabilidadExtra para el detalle de por qué se calculan aparte.
    public AnalisisResponse(AnalisisPerfil analisis, List<DetalleAnalisis> detalles,
                             List<HabilidadExtra> habilidadesExtra) {
        this.idAnalisis = analisis.getIdAnalisis();
        this.idPostulacion = analisis.getPostulacion().getIdPostulacion();
        this.porcentajeCoincidencia = analisis.getPorcentajeCoincidencia();
        this.clasificacion = analisis.getClasificacion();
        this.resumen = analisis.getResumen();
        this.fechaAnalisis = analisis.getFechaAnalisis();
        this.detalles = detalles;
        this.habilidadesExtra = habilidadesExtra;
    }
}
