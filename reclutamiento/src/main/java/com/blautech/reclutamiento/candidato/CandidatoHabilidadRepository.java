package com.blautech.reclutamiento.candidato;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidatoHabilidadRepository extends JpaRepository<CandidatoHabilidad, Integer> {
    List<CandidatoHabilidad> findByCandidato_IdCandidato(Integer idCandidato);
    boolean existsByCandidato_IdCandidatoAndHabilidad_IdHabilidad(Integer idCandidato, Integer idHabilidad);
}
