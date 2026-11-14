package com.blautech.reclutamiento.habilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HabilidadRepository extends JpaRepository<Habilidad, Integer> {
    Optional<Habilidad> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}
