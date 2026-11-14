package com.blautech.reclutamiento.candidato;

import com.blautech.reclutamiento.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidato")
@Getter
@Setter
@NoArgsConstructor
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_candidato")
    private Integer idCandidato;

    // @OneToOne porque la relación usuario-candidato es 1:0..1 (un
    // usuario tiene A LO MÁS un perfil de candidato). Como ya estamos
    // en un solo proyecto/una sola base de datos, esto SÍ puede ser una
    // relación JPA real con FK física (a diferencia de cuando esto era
    // un microservicio aparte).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "descripcion_perfil", columnDefinition = "TEXT")
    private String descripcionPerfil;

    @Column(name = "disponibilidad", length = 50)
    private String disponibilidad;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
}
