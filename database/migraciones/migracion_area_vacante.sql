-- ============================================================
-- Migración: agrega el catálogo de "área" y los campos nuevos
-- de vacante (área, conocimientos requeridos, estado BORRADOR)
-- que pide la pantalla "Crear vacante" alineada al prototipo.
--
-- Ejecuta este script UNA VEZ contra tu base blautech_bd antes
-- de reiniciar el backend (usa ddl-auto=validate, así que Spring
-- Boot no va a crear estas columnas solo; hay que aplicarlas a mano).
-- ============================================================

CREATE TABLE area (
    id_area    INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    estado     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT uq_area_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

INSERT INTO area (nombre) VALUES
    ('Tecnología'),
    ('Calidad'),
    ('Infraestructura'),
    ('Datos');

ALTER TABLE vacante
    ADD COLUMN id_area INT NULL AFTER id_usuario_creador,
    ADD COLUMN conocimientos_requeridos TEXT NULL AFTER descripcion;

ALTER TABLE vacante
    ADD CONSTRAINT fk_vacante_area FOREIGN KEY (id_area)
        REFERENCES area (id_area)
        ON UPDATE CASCADE ON DELETE SET NULL;

CREATE INDEX idx_vacante_area ON vacante (id_area);

-- No hace falta tocar la columna "estado": ya es VARCHAR(20) y ahora
-- simplemente también puede valer 'BORRADOR' además de
-- 'ACTIVA' / 'CERRADA' / 'CANCELADA'.
