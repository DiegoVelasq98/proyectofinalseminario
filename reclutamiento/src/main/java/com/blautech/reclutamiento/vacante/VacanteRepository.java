package com.blautech.reclutamiento.vacante;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacanteRepository extends JpaRepository<Vacante, Integer> {
    List<Vacante> findByEstado(String estado);
    List<Vacante> findByUsuarioCreador_IdUsuario(Integer idUsuario);
}
