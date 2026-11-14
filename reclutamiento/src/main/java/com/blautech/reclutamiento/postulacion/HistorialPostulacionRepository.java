package com.blautech.reclutamiento.postulacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialPostulacionRepository extends JpaRepository<HistorialPostulacion, Integer> {
    List<HistorialPostulacion> findByPostulacion_IdPostulacionOrderByFechaCambioAsc(Integer idPostulacion);

    // Todas las transiciones hacia un estado en particular (ej. CONTRATADO),
    // para calcular estadísticas agregadas sin tener que traer el historial
    // postulación por postulación.
    List<HistorialPostulacion> findByEstadoNuevo(String estadoNuevo);
}
