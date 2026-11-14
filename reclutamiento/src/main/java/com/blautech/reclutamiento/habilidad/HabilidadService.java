package com.blautech.reclutamiento.habilidad;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabilidadService {

    private final HabilidadRepository habilidadRepository;

    public List<Habilidad> listarTodas() {
        return habilidadRepository.findAll();
    }

    public Habilidad crear(String nombre, String descripcion) {
        if (habilidadRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe una habilidad con ese nombre");
        }
        Habilidad habilidad = new Habilidad();
        habilidad.setNombre(nombre);
        habilidad.setDescripcion(descripcion);
        return habilidadRepository.save(habilidad);
    }
}
