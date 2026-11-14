package com.blautech.reclutamiento.candidato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperienciaLaboralRepository extends JpaRepository<ExperienciaLaboral, Integer> {
    List<ExperienciaLaboral> findByCandidato_IdCandidato(Integer idCandidato);
}
