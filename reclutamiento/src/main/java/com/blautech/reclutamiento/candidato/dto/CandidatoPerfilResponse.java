package com.blautech.reclutamiento.candidato.dto;


import com.blautech.reclutamiento.candidato.Candidato;
import com.blautech.reclutamiento.candidato.CandidatoHabilidad;
import com.blautech.reclutamiento.candidato.Curriculum;
import com.blautech.reclutamiento.candidato.Educacion;
import com.blautech.reclutamiento.candidato.ExperienciaLaboral;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// La "foto completa" del perfil de un candidato: sus datos generales
// + las listas de experiencia, educación y habilidades que tenga
// registradas. Es lo que Angular pinta en la pantalla de "Mi perfil" Y
// en el detalle que ve RRHH (teléfono y currículum se agregaron para
// que RRHH pueda verlos también, ya vivían en la base de datos).
@Getter
@Setter
public class CandidatoPerfilResponse {
    private Integer idCandidato;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String ciudad;
    private String pais;
    private String descripcionPerfil;
    private String disponibilidad;

    private List<ExperienciaLaboral> experiencias;
    private List<Educacion> educaciones;
    private List<CandidatoHabilidadResponse> habilidades;

    // Del currículum más reciente que haya registrado (si tiene alguno).
    private String curriculumNombreArchivo;
    private LocalDateTime curriculumFechaCarga;

    public CandidatoPerfilResponse(Candidato c, List<ExperienciaLaboral> experiencias,
                                   List<Educacion> educaciones, List<CandidatoHabilidad> habilidades,
                                   List<Curriculum> curriculums) {
        this.idCandidato = c.getIdCandidato();
        this.nombre = c.getUsuario().getNombre();
        this.apellido = c.getUsuario().getApellido();
        this.correo = c.getUsuario().getCorreo();
        this.telefono = c.getUsuario().getTelefono();
        this.fechaNacimiento = c.getFechaNacimiento();
        this.direccion = c.getDireccion();
        this.ciudad = c.getCiudad();
        this.pais = c.getPais();
        this.descripcionPerfil = c.getDescripcionPerfil();
        this.disponibilidad = c.getDisponibilidad();
        this.experiencias = experiencias;
        this.educaciones = educaciones;
        this.habilidades = habilidades.stream().map(CandidatoHabilidadResponse::new).toList();

        curriculums.stream()
                .max(java.util.Comparator.comparing(Curriculum::getFechaCarga))
                .ifPresent(ultimo -> {
                    this.curriculumNombreArchivo = ultimo.getNombreArchivo();
                    this.curriculumFechaCarga = ultimo.getFechaCarga();
                });
    }
}
