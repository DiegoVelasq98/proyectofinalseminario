package com.blautech.reclutamiento.candidato;


import com.blautech.reclutamiento.analisis.ia.GeminiClient;
import com.blautech.reclutamiento.candidato.dto.*;
import com.blautech.reclutamiento.habilidad.Habilidad;
import com.blautech.reclutamiento.habilidad.HabilidadRepository;
import com.blautech.reclutamiento.usuario.Usuario;
import com.blautech.reclutamiento.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final CurriculumRepository curriculumRepository;
    private final ExperienciaLaboralRepository experienciaLaboralRepository;
    private final EducacionRepository educacionRepository;
    private final CandidatoHabilidadRepository candidatoHabilidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final HabilidadRepository habilidadRepository;
    private final GeminiClient geminiClient;

    @Value("${blautech.curriculums.directorio:archivos/curriculums}")
    private String directorioCurriculums;

    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB, igual que el prototipo
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "docx");

    // ------------------------------------------------------------------
    // Este método se repite en CADA operación de este Service: a partir
    // del correo que viene del token JWT (lo pone JwtAuthFilter en el
    // SecurityContext), buscamos primero el Usuario y luego su Candidato.
    // Lo centralizamos aquí para no repetir esta búsqueda en cada método.
    // ------------------------------------------------------------------
    private Candidato obtenerCandidatoPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        return candidatoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Este usuario todavía no tiene un perfil de candidato creado"));
    }

    // ------------------------------------------------------------------
    // Crear el perfil de candidato (una sola vez, la primera vez que el
    // usuario completa sus datos).
    // ------------------------------------------------------------------
    public CandidatoPerfilResponse crearPerfil(String correo, CandidatoPerfilRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        if (candidatoRepository.existsByUsuario_IdUsuario(usuario.getIdUsuario())) {
            throw new IllegalArgumentException("Este usuario ya tiene un perfil de candidato creado");
        }

        actualizarTelefonoSiVino(usuario, request.getTelefono());

        Candidato candidato = new Candidato();
        candidato.setUsuario(usuario);
        candidato.setFechaNacimiento(request.getFechaNacimiento());
        candidato.setDireccion(request.getDireccion());
        candidato.setCiudad(request.getCiudad());
        candidato.setPais(request.getPais());
        candidato.setDescripcionPerfil(request.getDescripcionPerfil());
        candidato.setDisponibilidad(request.getDisponibilidad());

        Candidato guardado = candidatoRepository.save(candidato);
        return armarRespuesta(guardado);
    }

    public CandidatoPerfilResponse obtenerPerfil(String correo) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        return armarRespuesta(candidato);
    }

    public CandidatoPerfilResponse actualizarPerfil(String correo, CandidatoPerfilRequest request) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);

        actualizarTelefonoSiVino(candidato.getUsuario(), request.getTelefono());

        candidato.setFechaNacimiento(request.getFechaNacimiento());
        candidato.setDireccion(request.getDireccion());
        candidato.setCiudad(request.getCiudad());
        candidato.setPais(request.getPais());
        candidato.setDescripcionPerfil(request.getDescripcionPerfil());
        candidato.setDisponibilidad(request.getDisponibilidad());
        candidato.setFechaActualizacion(LocalDateTime.now());

        Candidato actualizado = candidatoRepository.save(candidato);
        return armarRespuesta(actualizado);
    }

    // El teléfono vive en Usuario, no en Candidato -- por eso se guarda
    // aparte con su propio repositorio en vez de con candidatoRepository.
    private void actualizarTelefonoSiVino(Usuario usuario, String telefono) {
        if (telefono != null && !telefono.equals(usuario.getTelefono())) {
            usuario.setTelefono(telefono);
            usuarioRepository.save(usuario);
        }
    }

    // ------------------------------------------------------------------
    // Experiencia laboral
    // ------------------------------------------------------------------
    public ExperienciaLaboral agregarExperiencia(String correo, ExperienciaLaboralRequest request) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);

        ExperienciaLaboral experiencia = new ExperienciaLaboral();
        experiencia.setCandidato(candidato);
        experiencia.setEmpresa(request.getEmpresa());
        experiencia.setPuesto(request.getPuesto());
        experiencia.setDescripcion(request.getDescripcion());
        experiencia.setFechaInicio(request.getFechaInicio());
        experiencia.setFechaFin(request.getFechaFin());
        experiencia.setActualmenteTrabaja(request.isActualmenteTrabaja());

        return experienciaLaboralRepository.save(experiencia);
    }

    // ------------------------------------------------------------------
    // Educación
    // ------------------------------------------------------------------
    public Educacion agregarEducacion(String correo, EducacionRequest request) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);

        Educacion educacion = new Educacion();
        educacion.setCandidato(candidato);
        educacion.setInstitucion(request.getInstitucion());
        educacion.setTitulo(request.getTitulo());
        educacion.setNivelAcademico(request.getNivelAcademico());
        educacion.setFechaInicio(request.getFechaInicio());
        educacion.setFechaFin(request.getFechaFin());
        if (request.getEstado() != null) {
            educacion.setEstado(request.getEstado());
        }

        return educacionRepository.save(educacion);
    }

    // Corregir un registro de experiencia ya guardado (ej. una fecha o
    // un puesto con error de dedo) sin tener que borrarlo y crearlo de
    // nuevo -- mismo patrón de validación de dueño que eliminarHabilidad().
    public ExperienciaLaboral actualizarExperiencia(String correo, Integer idExperiencia,
                                                      ExperienciaLaboralRequest request) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        ExperienciaLaboral experiencia = experienciaLaboralRepository.findById(idExperiencia)
                .orElseThrow(() -> new IllegalArgumentException("Esa experiencia no existe en tu perfil"));
        if (!experiencia.getCandidato().getIdCandidato().equals(candidato.getIdCandidato())) {
            throw new IllegalArgumentException("Esa experiencia no pertenece a tu perfil");
        }

        experiencia.setEmpresa(request.getEmpresa());
        experiencia.setPuesto(request.getPuesto());
        experiencia.setDescripcion(request.getDescripcion());
        experiencia.setFechaInicio(request.getFechaInicio());
        experiencia.setFechaFin(request.getFechaFin());
        experiencia.setActualmenteTrabaja(request.isActualmenteTrabaja());

        return experienciaLaboralRepository.save(experiencia);
    }

    public void eliminarExperiencia(String correo, Integer idExperiencia) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        ExperienciaLaboral experiencia = experienciaLaboralRepository.findById(idExperiencia)
                .orElseThrow(() -> new IllegalArgumentException("Esa experiencia no existe en tu perfil"));
        if (!experiencia.getCandidato().getIdCandidato().equals(candidato.getIdCandidato())) {
            throw new IllegalArgumentException("Esa experiencia no pertenece a tu perfil");
        }
        experienciaLaboralRepository.delete(experiencia);
    }

    // Mismo par editar/eliminar, para educación.
    public Educacion actualizarEducacion(String correo, Integer idEducacion, EducacionRequest request) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        Educacion educacion = educacionRepository.findById(idEducacion)
                .orElseThrow(() -> new IllegalArgumentException("Esa educación no existe en tu perfil"));
        if (!educacion.getCandidato().getIdCandidato().equals(candidato.getIdCandidato())) {
            throw new IllegalArgumentException("Esa educación no pertenece a tu perfil");
        }

        educacion.setInstitucion(request.getInstitucion());
        educacion.setTitulo(request.getTitulo());
        educacion.setNivelAcademico(request.getNivelAcademico());
        educacion.setFechaInicio(request.getFechaInicio());
        educacion.setFechaFin(request.getFechaFin());
        if (request.getEstado() != null) {
            educacion.setEstado(request.getEstado());
        }

        return educacionRepository.save(educacion);
    }

    public void eliminarEducacion(String correo, Integer idEducacion) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        Educacion educacion = educacionRepository.findById(idEducacion)
                .orElseThrow(() -> new IllegalArgumentException("Esa educación no existe en tu perfil"));
        if (!educacion.getCandidato().getIdCandidato().equals(candidato.getIdCandidato())) {
            throw new IllegalArgumentException("Esa educación no pertenece a tu perfil");
        }
        educacionRepository.delete(educacion);
    }

    // ------------------------------------------------------------------
    // Habilidades del candidato
    // ------------------------------------------------------------------
    public CandidatoHabilidadResponse agregarHabilidad(String correo, CandidatoHabilidadRequest request) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);

        Habilidad habilidad = habilidadRepository.findById(request.getIdHabilidad())
                .orElseThrow(() -> new IllegalArgumentException("La habilidad indicada no existe"));

        if (candidatoHabilidadRepository.existsByCandidato_IdCandidatoAndHabilidad_IdHabilidad(
                candidato.getIdCandidato(), habilidad.getIdHabilidad())) {
            throw new IllegalArgumentException("Ya registraste esta habilidad en tu perfil");
        }

        CandidatoHabilidad candidatoHabilidad = new CandidatoHabilidad();
        candidatoHabilidad.setCandidato(candidato);
        candidatoHabilidad.setHabilidad(habilidad);
        candidatoHabilidad.setNivel(request.getNivel());
        candidatoHabilidad.setAniosExperiencia(request.getAniosExperiencia());

        CandidatoHabilidad guardada = candidatoHabilidadRepository.save(candidatoHabilidad);
        return new CandidatoHabilidadResponse(guardada);
    }

    public void eliminarHabilidad(String correo, Integer idCandidatoHabilidad) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        CandidatoHabilidad ch = candidatoHabilidadRepository.findById(idCandidatoHabilidad)
                .orElseThrow(() -> new IllegalArgumentException("Esa habilidad no existe en tu perfil"));
        if (!ch.getCandidato().getIdCandidato().equals(candidato.getIdCandidato())) {
            throw new IllegalArgumentException("Esa habilidad no pertenece a tu perfil");
        }
        candidatoHabilidadRepository.delete(ch);
    }

    private static final List<String> NIVELES_VALIDOS = List.of("BASICO", "INTERMEDIO", "AVANZADO");

    // PUT /candidatos/habilidades/{id} -> le pone (o le cambia) el nivel a
    // una habilidad que el candidato YA tiene en su perfil, sin tener que
    // borrarla y volverla a agregar. Lo usa, sobre todo, para las
    // habilidades que detectó el currículum (quedan sin nivel porque ese
    // dato no se puede "leer" del documento -- ver registrarYProcesarCurriculum).
    public CandidatoHabilidadResponse actualizarNivelHabilidad(String correo, Integer idCandidatoHabilidad,
                                                                ActualizarNivelHabilidadRequest request) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        CandidatoHabilidad ch = candidatoHabilidadRepository.findById(idCandidatoHabilidad)
                .orElseThrow(() -> new IllegalArgumentException("Esa habilidad no existe en tu perfil"));
        if (!ch.getCandidato().getIdCandidato().equals(candidato.getIdCandidato())) {
            throw new IllegalArgumentException("Esa habilidad no pertenece a tu perfil");
        }
        if (!NIVELES_VALIDOS.contains(request.getNivel())) {
            throw new IllegalArgumentException("El nivel debe ser BASICO, INTERMEDIO o AVANZADO");
        }

        ch.setNivel(request.getNivel());
        CandidatoHabilidad guardada = candidatoHabilidadRepository.save(ch);
        return new CandidatoHabilidadResponse(guardada);
    }

    // ------------------------------------------------------------------
    // Currículum: subida real del archivo (PDF/DOCX) + lectura de su
    // texto para detectar, contra el catálogo REAL de habilidades,
    // cuáles se mencionan en el documento. No hay ningún motor de IA
    // todavía (eso está planeado para más adelante con n8n) -- esto es
    // coincidencia de texto simple y verificable, no una predicción.
    // ------------------------------------------------------------------
    public CurriculumProcesadoResponse registrarYProcesarCurriculum(String correo, MultipartFile archivo) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el máximo permitido de 5 MB");
        }
        String nombreOriginal = archivo.getOriginalFilename() != null && !archivo.getOriginalFilename().isBlank()
                ? archivo.getOriginalFilename() : "curriculum";
        String extension = obtenerExtension(nombreOriginal);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF o DOCX");
        }

        String textoExtraido;
        try {
            textoExtraido = extraerTexto(archivo, extension);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el contenido del archivo", e);
        }

        String rutaGuardada;
        try {
            rutaGuardada = guardarEnDisco(archivo, extension);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo en el servidor", e);
        }

        Curriculum curriculum = new Curriculum();
        curriculum.setCandidato(candidato);
        curriculum.setNombreArchivo(nombreOriginal);
        curriculum.setRutaArchivo(rutaGuardada);
        curriculum.setTipoArchivo(extension);
        curriculum.setEstadoProcesamiento("COMPLETADO");
        curriculumRepository.save(curriculum);

        // Habilidades detectadas de verdad en el texto del documento. Desde
        // que existe la extraccion con IA, ESA es la fuente autoritativa:
        // Gemini lee el curriculum completo y devuelve TODAS las habilidades
        // que el candidato menciona, esten o no ya en el catalogo -- por eso
        // cada una de ellas (las 10, las 20, las que sean) se registra en su
        // perfil, sin filtrar por si ya existian o no. El catalogo solo entra
        // para decidir si hay que crear la habilidad de cero o reutilizar la
        // que ya existe. La coincidencia simple por texto contra el catalogo
        // (lo que habia antes de la IA) queda como respaldo unicamente para
        // cuando Gemini no esta disponible o falla -- nunca debe romper la
        // subida del curriculum.
        List<Habilidad> catalogo = habilidadRepository.findAll();
        List<String> detectadas = new ArrayList<>();
        List<String> agregadas = new ArrayList<>();
        List<String> nuevasEnCatalogo = new ArrayList<>();
        boolean detectadoConIA = false;

        if (geminiClient.estaDisponible()) {
            try {
                List<Habilidad> catalogoActual = new ArrayList<>(catalogo);
                List<String> habilidadesIA = geminiClient.extraerHabilidadesDeTexto(textoExtraido);

                for (String nombreIA : habilidadesIA) {
                    if (nombreIA == null || nombreIA.isBlank()) {
                        continue;
                    }
                    String nombreLimpio = nombreIA.trim();
                    String nombreNormalizado = nombreLimpio.toLowerCase(Locale.forLanguageTag("es"));

                    Habilidad habilidad = catalogoActual.stream()
                            .filter(h -> h.getNombre().toLowerCase(Locale.forLanguageTag("es")).equals(nombreNormalizado))
                            .findFirst()
                            .orElse(null);

                    if (habilidad == null) {
                        // No existia en el catalogo -> se crea, igual que si
                        // un Administrador la hubiera agregado manualmente.
                        // Asi, sea cual sea la habilidad que traiga el CV,
                        // siempre queda un registro al que asociarla.
                        habilidad = new Habilidad();
                        habilidad.setNombre(nombreLimpio);
                        habilidad.setDescripcion("Detectada automaticamente desde un curriculum");
                        habilidad = habilidadRepository.save(habilidad);
                        catalogoActual.add(habilidad);
                        nuevasEnCatalogo.add(habilidad.getNombre());
                    }

                    boolean yaLaTiene = candidatoHabilidadRepository.existsByCandidato_IdCandidatoAndHabilidad_IdHabilidad(
                            candidato.getIdCandidato(), habilidad.getIdHabilidad());
                    if (!yaLaTiene) {
                        CandidatoHabilidad ch = new CandidatoHabilidad();
                        ch.setCandidato(candidato);
                        ch.setHabilidad(habilidad);
                        candidatoHabilidadRepository.save(ch);
                        agregadas.add(habilidad.getNombre());
                    }
                    if (!detectadas.contains(habilidad.getNombre())) {
                        detectadas.add(habilidad.getNombre());
                    }
                }

                detectadoConIA = true;
            } catch (Exception e) {
                // No se pudo extraer con IA (sin internet, API caida, etc.) --
                // seguimos abajo con el respaldo por catalogo, nunca se
                // interrumpe el flujo de subida del curriculum.
            }
        }

        // Respaldo: coincidencia de texto simple contra el catalogo. Solo se
        // usa si la IA no esta disponible o fallo -- cuando si funciono, ya
        // cubrio absolutamente todo lo que habia que detectar.
        if (!detectadoConIA) {
            String textoNormalizado = textoExtraido.toLowerCase(Locale.forLanguageTag("es"));
            for (Habilidad h : catalogo) {
                String nombreNormalizado = h.getNombre().toLowerCase(Locale.forLanguageTag("es"));
                if (contieneComoPalabra(textoNormalizado, nombreNormalizado)) {
                    detectadas.add(h.getNombre());
                    boolean yaLaTiene = candidatoHabilidadRepository.existsByCandidato_IdCandidatoAndHabilidad_IdHabilidad(
                            candidato.getIdCandidato(), h.getIdHabilidad());
                    if (!yaLaTiene) {
                        CandidatoHabilidad ch = new CandidatoHabilidad();
                        ch.setCandidato(candidato);
                        ch.setHabilidad(h);
                        candidatoHabilidadRepository.save(ch);
                        agregadas.add(h.getNombre());
                    }
                }
            }
        }

        List<Educacion> educaciones = educacionRepository.findByCandidato_IdCandidato(candidato.getIdCandidato());
        List<ExperienciaLaboral> experiencias = experienciaLaboralRepository.findByCandidato_IdCandidato(candidato.getIdCandidato());

        return new CurriculumProcesadoResponse(
                curriculum.getIdCurriculum(),
                nombreOriginal,
                curriculum.getFechaCarga(),
                candidato.getUsuario().getNombre() + " " + candidato.getUsuario().getApellido(),
                formacionPrincipal(educaciones),
                aniosExperienciaTexto(experiencias),
                formacionPrincipal(educaciones),
                detectadas,
                agregadas,
                nuevasEnCatalogo
        );
    }

    private String obtenerExtension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        if (punto < 0 || punto == nombreArchivo.length() - 1) return "";
        return nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private String extraerTexto(MultipartFile archivo, String extension) throws IOException {
        try (InputStream in = archivo.getInputStream()) {
            if ("pdf".equals(extension)) {
                try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
                    return new PDFTextStripper().getText(doc);
                }
            } else {
                try (XWPFDocument doc = new XWPFDocument(in);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            }
        }
    }

    private String guardarEnDisco(MultipartFile archivo, String extension) throws IOException {
        Path carpeta = Path.of(directorioCurriculums);
        Files.createDirectories(carpeta);
        String nombreEnDisco = UUID.randomUUID() + "." + extension;
        Path destino = carpeta.resolve(nombreEnDisco);
        archivo.transferTo(destino);
        return destino.toString();
    }

    // Coincidencia de "palabra completa" para no confundir, por ejemplo,
    // la habilidad "Go" con la palabra "algo". Usamos lookaround en vez
    // de \b porque \b exige un carácter alfanumérico de un lado -- con
    // nombres como "C++" o "C#" (terminan en símbolo) \b nunca calzaría
    // bien. Aquí solo exigimos que NO haya una letra/dígito pegado antes
    // o después, que es lo que realmente queremos evitar (coincidencias
    // parciales dentro de otra palabra). Pattern.quote evita que nombres
    // como "C++" o "C#" rompan la expresión regular.
    private boolean contieneComoPalabra(String textoNormalizado, String palabra) {
        String patron = "(?<![a-z0-9])" + Pattern.quote(palabra) + "(?![a-z0-9])";
        return Pattern.compile(patron).matcher(textoNormalizado).find();
    }

    // La formación más reciente (por fecha de inicio) -- se usa igual
    // que en el frontend para mostrar la "profesión" del candidato.
    private String formacionPrincipal(List<Educacion> educaciones) {
        if (educaciones == null || educaciones.isEmpty()) return "Sin formación registrada";
        return educaciones.stream()
                .max(Comparator.comparing(Educacion::getFechaInicio, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Educacion::getTitulo)
                .orElse("Sin formación registrada");
    }

    private String aniosExperienciaTexto(List<ExperienciaLaboral> experiencias) {
        if (experiencias == null || experiencias.isEmpty()) return "Sin experiencia registrada";
        long totalDias = 0;
        for (ExperienciaLaboral e : experiencias) {
            LocalDate inicio = e.getFechaInicio();
            LocalDate fin = (e.getFechaFin() != null) ? e.getFechaFin() : LocalDate.now();
            if (inicio != null && fin.isAfter(inicio)) {
                totalDias += java.time.temporal.ChronoUnit.DAYS.between(inicio, fin);
            }
        }
        long anios = Math.round(totalDias / 365.25);
        return anios + (anios == 1 ? " año" : " años");
    }

    // ------------------------------------------------------------------
    // Descarga del archivo real del currículum (para el propio candidato
    // o para RRHH revisando un candidato en particular).
    // ------------------------------------------------------------------
    public ArchivoCurriculum obtenerArchivoCurriculumPropio(String correo) {
        Candidato candidato = obtenerCandidatoPorCorreo(correo);
        return leerArchivoMasReciente(candidato.getIdCandidato());
    }

    public ArchivoCurriculum obtenerArchivoCurriculumDeCandidato(Integer idCandidato) {
        if (!candidatoRepository.existsById(idCandidato)) {
            throw new IllegalArgumentException("El candidato indicado no existe");
        }
        return leerArchivoMasReciente(idCandidato);
    }

    private ArchivoCurriculum leerArchivoMasReciente(Integer idCandidato) {
        Curriculum curriculum = curriculumRepository.findByCandidato_IdCandidato(idCandidato).stream()
                .max(Comparator.comparing(Curriculum::getFechaCarga))
                .orElseThrow(() -> new IllegalArgumentException("Este candidato todavía no ha registrado un currículum"));
        try {
            byte[] contenido = Files.readAllBytes(Path.of(curriculum.getRutaArchivo()));
            return new ArchivoCurriculum(contenido, curriculum.getNombreArchivo(), curriculum.getTipoArchivo());
        } catch (IOException e) {
            throw new IllegalStateException("El archivo ya no está disponible en el servidor", e);
        }
    }

    public record ArchivoCurriculum(byte[] contenido, String nombreArchivo, String tipoArchivo) {}

    // ------------------------------------------------------------------
    // Directorio de candidatos para RRHH. Reutiliza el mismo "armado" de
    // respuesta que usa el candidato para ver su propio perfil -- la
    // diferencia es solo quién pregunta y que aquí se listan todos.
    // ------------------------------------------------------------------
    public List<CandidatoPerfilResponse> listarTodos() {
        return candidatoRepository.findAll().stream()
                .map(this::armarRespuesta)
                .toList();
    }

    public CandidatoPerfilResponse obtenerPorId(Integer idCandidato) {
        Candidato candidato = candidatoRepository.findById(idCandidato)
                .orElseThrow(() -> new IllegalArgumentException("El candidato indicado no existe"));
        return armarRespuesta(candidato);
    }

    // ------------------------------------------------------------------
    private CandidatoPerfilResponse armarRespuesta(Candidato candidato) {
        var experiencias = experienciaLaboralRepository.findByCandidato_IdCandidato(candidato.getIdCandidato());
        var educaciones = educacionRepository.findByCandidato_IdCandidato(candidato.getIdCandidato());
        var habilidades = candidatoHabilidadRepository.findByCandidato_IdCandidato(candidato.getIdCandidato());
        var curriculums = curriculumRepository.findByCandidato_IdCandidato(candidato.getIdCandidato());
        return new CandidatoPerfilResponse(candidato, experiencias, educaciones, habilidades, curriculums);
    }
}
