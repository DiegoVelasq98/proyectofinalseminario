package com.blautech.reclutamiento.area;

import com.blautech.reclutamiento.area.dto.AreaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Catálogo de áreas: antes solo se podían LEER (vía /areas, para llenar
// el <select> del formulario de vacante) -- si RRHH necesitaba una
// nueva, había que insertarla directo en la base de datos. Este
// Service agrega crear/editar/desactivar, reutilizando el mismo
// patrón de "estado" (ACTIVO/INACTIVO) que ya usan Usuario y Vacante
// en vez de un DELETE físico: así un área que ya está en uso por
// vacantes existentes nunca se pierde ni rompe esas referencias,
// simplemente deja de aparecer como opción para vacantes NUEVAS.
@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;

    public List<Area> listarActivas() {
        return areaRepository.findByEstado("ACTIVO");
    }

    public List<Area> listarTodas() {
        return areaRepository.findAll();
    }

    public Area crear(AreaRequest request) {
        if (areaRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un área con ese nombre");
        }
        Area area = new Area();
        area.setNombre(request.getNombre());
        return areaRepository.save(area);
    }

    public Area actualizar(Integer idArea, AreaRequest request) {
        Area area = buscarPorId(idArea);
        if (!area.getNombre().equals(request.getNombre()) && areaRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un área con ese nombre");
        }
        area.setNombre(request.getNombre());
        return areaRepository.save(area);
    }

    public Area desactivar(Integer idArea) {
        Area area = buscarPorId(idArea);
        area.setEstado("INACTIVO");
        return areaRepository.save(area);
    }

    public Area activar(Integer idArea) {
        Area area = buscarPorId(idArea);
        area.setEstado("ACTIVO");
        return areaRepository.save(area);
    }

    private Area buscarPorId(Integer idArea) {
        return areaRepository.findById(idArea)
                .orElseThrow(() -> new IllegalArgumentException("El área indicada no existe"));
    }
}
