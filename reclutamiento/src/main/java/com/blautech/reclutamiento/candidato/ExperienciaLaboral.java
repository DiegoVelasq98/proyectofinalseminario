package com.blautech.reclutamiento.candidato;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "experiencia_laboral")
@Getter
@Setter
@NoArgsConstructor
public class ExperienciaLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_experiencia")
    private Integer idExperiencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidato", nullable = false)
    private Candidato candidato;

    @Column(name = "empresa", nullable = false, length = 150)
    private String empresa;

    @Column(name = "puesto", nullable = false, length = 150)
    private String puesto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "actualmente_trabaja", nullable = false)
    private Boolean actualmenteTrabaja = false;
}
