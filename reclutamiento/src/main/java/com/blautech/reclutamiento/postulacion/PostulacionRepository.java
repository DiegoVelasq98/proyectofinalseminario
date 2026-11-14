package com.blautech.reclutamiento.postulacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostulacionRepository extends JpaRepository<Postulacion, Integer> {
    List<Postulacion> findByCandidato_IdCandidato(Integer idCandidato);
    List<Postulacion> findByVacante_IdVacante(Integer idVacante);
    boolean existsByCandidato_IdCandidatoAndVacante_IdVacante(Integer idCandidato, Integer idVacante);
}
