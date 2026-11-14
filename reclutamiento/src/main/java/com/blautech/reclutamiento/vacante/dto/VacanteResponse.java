package com.blautech.reclutamiento.vacante.dto;

import com.blautech.reclutamiento.vacante.Vacante;
import com.blautech.reclutamiento.vacante.VacanteHabilidad;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class VacanteResponse {
    private Integer idVacante;
    private Integer idArea;
    private String areaNombre;
    private String titulo;
    private String descripcion;
    private String conocimientosRequeridos;
    private String ubicacion;
    private String modalidad;
    private String experienciaRequerida;
    private String nivelAcademico;
    private LocalDateTime fechaPublicacion;
    private LocalDate fechaCierre;
    private String estado;
    private Integer idUsuarioCreador; // id del responsable actual (para preseleccionar el <select>)
    private String creadoPor; // nombre del usuario RRHH que la creó / responsable actual
    private List<VacanteHabilidadResponse> habilidadesRequeridas;

    public VacanteResponse(Vacante v, List<VacanteHabilidad> habilidadesRequeridas) {
        this.idVacante = v.getIdVacante();
        if (v.getArea() != null) {
            this.idArea = v.getArea().getIdArea();
            this.areaNombre = v.getArea().getNombre();
        }
        this.titulo = v.getTitulo();
        this.descripcion = v.getDescripcion();
        this.conocimientosRequeridos = v.getConocimientosRequeridos();
        this.ubicacion = v.getUbicacion();
        this.modalidad = v.getModalidad();
        this.experienciaRequerida = v.getExperienciaRequerida();
        this.nivelAcademico = v.getNivelAcademico();
        this.fechaPublicacion = v.getFechaPublicacion();
        this.fechaCierre = v.getFechaCierre();
        this.estado = v.getEstado();
        this.idUsuarioCreador = v.getUsuarioCreador().getIdUsuario();
        this.creadoPor = v.getUsuarioCreador().getNombre() + " " + v.getUsuarioCreador().getApellido();
        this.habilidadesRequeridas = habilidadesRequeridas.stream().map(VacanteHabilidadResponse::new).toList();
    }
}
