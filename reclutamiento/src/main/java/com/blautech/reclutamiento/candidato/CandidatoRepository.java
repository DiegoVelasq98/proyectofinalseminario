package com.blautech.reclutamiento.candidato;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidatoRepository extends JpaRepository<Candidato, Integer> {

    // La usamos constantemente: "dame el perfil de candidato del usuario
    // que tiene este id". Spring Data JPA entiende "Usuario_IdUsuario"
    // como "entra al campo 'usuario' del Candidato, y de ahí a su campo
    // 'idUsuario'" -- así se navegan relaciones dentro del nombre del método.
    Optional<Candidato> findByUsuario_IdUsuario(Integer idUsuario);

    boolean existsByUsuario_IdUsuario(Integer idUsuario);
}
