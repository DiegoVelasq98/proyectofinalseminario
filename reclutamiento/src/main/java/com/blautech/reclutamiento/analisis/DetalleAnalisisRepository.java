package com.blautech.reclutamiento.analisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleAnalisisRepository extends JpaRepository<DetalleAnalisis, Integer> {
    List<DetalleAnalisis> findByAnalisisPerfil_IdAnalisis(Integer idAnalisis);
}
