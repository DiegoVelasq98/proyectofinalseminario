package com.blautech.reclutamiento.candidato;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "educacion")
@Getter
@Setter
@NoArgsConstructor
public class Educacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_educacion")
    private Integer idEducacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidato", nullable = false)
    private Candidato candidato;

    @Column(name = "institucion", nullable = false, length = 150)
    private String institucion;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "nivel_academico", length = 50)
    private String nivelAcademico;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "EN_CURSO";
}
