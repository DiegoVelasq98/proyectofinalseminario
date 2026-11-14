package com.blautech.reclutamiento.candidato.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// Lo que Angular manda al crear o actualizar el perfil del candidato.
// Todo es opcional aquí porque un perfil se puede ir completando poco
// a poco (no se exige llenarlo todo de una vez).
@Getter
@Setter
public class CandidatoPerfilRequest {
    private String telefono;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String ciudad;
    private String pais;
    private String descripcionPerfil;
    private String disponibilidad;
}
