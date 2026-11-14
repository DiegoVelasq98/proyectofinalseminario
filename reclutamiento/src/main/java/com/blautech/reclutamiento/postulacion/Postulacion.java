package com.blautech.reclutamiento.postulacion;


import com.blautech.reclutamiento.candidato.Candidato;
import com.blautech.reclutamiento.vacante.Vacante;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "postulacion")
@Getter
@Setter
@NoArgsConstructor
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion")
    private Integer idPostulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidato", nullable = false)
    private Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vacante", nullable = false)
    private Vacante vacante;

    @Column(name = "fecha_postulacion", nullable = false)
    private LocalDateTime fechaPostulacion = LocalDateTime.now();

    // RECIBIDA / EN_REVISION / PREOSELECCIONADO / RECHAZADA / CONTRATADO
    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "RECIBIDA";
}
