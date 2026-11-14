package com.blautech.reclutamiento.vacante;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacanteHabilidadRepository extends JpaRepository<VacanteHabilidad, Integer> {
    List<VacanteHabilidad> findByVacante_IdVacante(Integer idVacante);
    boolean existsByVacante_IdVacanteAndHabilidad_IdHabilidad(Integer idVacante, Integer idHabilidad);
}
