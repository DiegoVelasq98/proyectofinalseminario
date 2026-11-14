package com.blautech.reclutamiento.candidato;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "curriculum")
@Getter
@Setter
@NoArgsConstructor
public class Curriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curriculum")
    private Integer idCurriculum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidato", nullable = false)
    private Candidato candidato;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    // Por ahora guardamos solo la ruta/URL del archivo como texto. La
    // subida real de archivos (multipart) la dejamos para una siguiente
    // fase -- para este avance basta con registrar los datos.
    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tipo_archivo", length = 20)
    private String tipoArchivo;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();

    @Column(name = "estado_procesamiento", nullable = false, length = 20)
    private String estadoProcesamiento = "PENDIENTE";
}