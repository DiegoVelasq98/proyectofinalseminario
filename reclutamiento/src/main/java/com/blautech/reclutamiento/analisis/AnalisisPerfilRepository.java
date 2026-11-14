package com.blautech.reclutamiento.analisis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalisisPerfilRepository extends JpaRepository<AnalisisPerfil, Integer> {
    List<AnalisisPerfil> findByPostulacion_IdPostulacionOrderByFechaAnalisisDesc(Integer idPostulacion);
}
