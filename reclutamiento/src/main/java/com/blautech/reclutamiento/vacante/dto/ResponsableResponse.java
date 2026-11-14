package com.blautech.reclutamiento.vacante.dto;

import com.blautech.reclutamiento.usuario.Usuario;
import lombok.Getter;
import lombok.Setter;

// Lista liviana de usuarios RRHH activos, para que RRHH elija a quién
// asignarle una vacante como responsable (selector en el frontend). No
// expone correo ni ningún otro dato sensible, solo lo necesario para
// mostrarlo en un <select>.
@Getter
@Setter
public class ResponsableResponse {
    private Integer idUsuario;
    private String nombreCompleto;

    public ResponsableResponse(Usuario u) {
        this.idUsuario = u.getIdUsuario();
        this.nombreCompleto = u.getNombre() + " " + u.getApellido();
    }
}
