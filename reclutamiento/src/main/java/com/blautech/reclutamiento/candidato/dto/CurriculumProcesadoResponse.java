package com.blautech.reclutamiento.candidato.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// Lo que ve el candidato justo después de registrar su currículum.
// IMPORTANTE: nombre/profesión/experiencia/educación NO se "adivinan"
// leyendo el PDF -- son los datos que el candidato ya tiene guardados
// en su perfil (por eso es un resumen confiable). Lo que SÍ se lee de
// verdad del documento son las "habilidadesDetectadas": el texto del
// archivo se compara contra el catálogo real de habilidades y solo se
// listan las que de verdad aparecen mencionadas ahí.
@Getter
@AllArgsConstructor
public class CurriculumProcesadoResponse {
    private Integer idCurriculum;
    private String nombreArchivo;
    private LocalDateTime fechaCarga;

    private String nombreCompleto;
    private String profesion;
    private String experiencia;
    private String educacion;

    private List<String> habilidadesDetectadas;
    // De las detectadas, cuáles no estaban ya en su perfil y se
    // agregaron automáticamente (para que las vea reflejado ahí mismo).
    private List<String> habilidadesAgregadasAlPerfil;
    // De las agregadas, cuáles no existían en el catálogo de la
    // plataforma y se crearon automáticamente a partir de lo que Gemini
    // detectó en el texto del currículum (ninguna estaba "grabada" antes).
    private List<String> habilidadesNuevasEnCatalogo;
}
