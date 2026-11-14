package com.blautech.reclutamiento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice = "esto aplica a TODOS los Controllers del
// proyecto". En vez de poner un try/catch en cada método de cada
// Controller, centralizamos aquí cómo se ve un error para el cliente
// (Angular). Esto evita repetir código y mantiene las respuestas de
// error consistentes en todo el sistema.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Se dispara cuando el Service lanza IllegalArgumentException
    // (ej. "correo ya registrado", "correo o contraseña incorrectos").
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarIllegalArgument(IllegalArgumentException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Se dispara cuando el Service lanza IllegalStateException
    // (ej. "cuenta inactiva", "rol no configurado").
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> manejarIllegalState(IllegalStateException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Se dispara cuando el Service lanza AccessDeniedException: el usuario
    // SÍ está autenticado (si no, ni llegaría aquí -- Spring Security lo
    // habría rechazado antes con 401), pero no tiene permiso sobre ESE
    // dato en particular (ej. el historial de la postulación de otro
    // candidato). Es distinto de @PreAuthorize: ese bloquea por ROL antes
    // de entrar al método; esto bloquea por DUEÑO del dato, ya adentro.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccessDeniedException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Se dispara ANTES de que nuestro propio Service alcance a revisar el
    // tamaño (spring.servlet.multipart.max-file-size en application.properties
    // es el límite duro de Spring) -- por ejemplo, subiendo un currículum
    // enorme. Sin este handler, el error se iría como un 500 genérico sin
    // "mensaje", y Angular mostraría un mensaje menos claro.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> manejarArchivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El archivo supera el tamaño máximo permitido.");
    }

    // Se dispara automáticamente cuando falla una validación de @Valid
    // en un DTO (ej. @NotBlank, @Email, @Size). Recorremos todos los
    // campos que fallaron y armamos un mensaje claro para cada uno.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errores", errores);

        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
