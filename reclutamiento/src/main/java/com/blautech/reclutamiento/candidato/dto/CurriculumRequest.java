package com.blautech.reclutamiento.candidato.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Versión simplificada: por ahora registramos los DATOS del currículum
// (nombre y ruta del archivo), no la subida física del archivo en sí.
// La subida real con MultipartFile queda como una mejora para después
// -- no es indispensable para demostrar el requerimiento funcional.
@Getter
@Setter
public class CurriculumRequest {

    @NotBlank(message = "El nombre del archivo es obligatorio")
    private String nombreArchivo;

    @NotBlank(message = "La ruta del archivo es obligatoria")
    private String rutaArchivo;

    private String tipoArchivo; // pdf, docx, etc.
}
