package com.blautech.reclutamiento.analisis.dto;

// Habilidad que el candidato tiene registrada en su perfil pero que la
// vacante NO pidió. Es puramente informativa para RRHH -- "esto más
// sabe el candidato" -- y nunca participa en el cálculo del porcentaje
// de coincidencia ni en la clasificación (ALTA/MEDIA/BAJA), que siguen
// dependiendo únicamente de las habilidades que sí pidió la vacante.
public record HabilidadExtra(String nombre, String nivel) {
}
