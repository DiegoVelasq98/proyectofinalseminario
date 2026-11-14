package com.blautech.reclutamiento.area;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    List<Area> findByEstado(String estado);
    boolean existsByNombre(String nombre);
}
