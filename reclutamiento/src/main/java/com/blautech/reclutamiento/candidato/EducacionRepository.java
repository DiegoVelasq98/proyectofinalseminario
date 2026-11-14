package com.blautech.reclutamiento.candidato;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducacionRepository extends JpaRepository<Educacion, Integer> {
    List<Educacion> findByCandidato_IdCandidato(Integer idCandidato);
}
