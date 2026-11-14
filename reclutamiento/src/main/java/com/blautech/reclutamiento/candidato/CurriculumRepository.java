package com.blautech.reclutamiento.candidato;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculumRepository extends JpaRepository<Curriculum, Integer> {
    List<Curriculum> findByCandidato_IdCandidato(Integer idCandidato);
}
