package com.blautech.reclutamiento.candidato;

import com.blautech.reclutamiento.habilidad.Habilidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Esta es la tabla que resuelve la relación N:M entre Candidato y
// Habilidad (un candidato tiene muchas habilidades, una habilidad la
// pueden tener muchos candidatos). Vive aquí en "candidato" porque
// conceptualmente es "las habilidades DE un candidato".
@Entity
@Table(name = "candidato_habilidad")
@Getter
@Setter
@NoArgsConstructor
public class CandidatoHabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_candidato_habilidad")
    private Integer idCandidatoHabilidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidato", nullable = false)
    private Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habilidad", nullable = false)
    private Habilidad habilidad;

    @Column(name = "nivel", length = 20)
    private String nivel; // BASICO / INTERMEDIO / AVANZADO

    @Column(name = "anios_experiencia", precision = 4, scale = 1)
    private BigDecimal aniosExperiencia;
}
