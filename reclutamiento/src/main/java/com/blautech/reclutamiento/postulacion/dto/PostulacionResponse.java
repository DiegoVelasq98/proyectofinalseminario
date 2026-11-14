package com.blautech.reclutamiento.postulacion.dto;

import com.blautech.reclutamiento.postulacion.Postulacion;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostulacionResponse {
    private Integer idPostulacion;
    private Integer idVacante;
    private String tituloVacante;
    private Integer idCandidato;
    private String nombreCandidato;
    private LocalDateTime fechaPostulacion;
    private String estado;

    public PostulacionResponse(Postulacion p) {
        this.idPostulacion = p.getIdPostulacion();
        this.idVacante = p.getVacante().getIdVacante();
        this.tituloVacante = p.getVacante().getTitulo();
        this.idCandidato = p.getCandidato().getIdCandidato();
        this.nombreCandidato = p.getCandidato().getUsuario().getNombre() + " "
                + p.getCandidato().getUsuario().getApellido();
        this.fechaPostulacion = p.getFechaPostulacion();
        this.estado = p.getEstado();
    }
}