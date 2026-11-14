-- ============================================================
-- Migración: agrega la bandera que obliga a cambiar la
-- contraseña en el próximo login. Se activa cuando el
-- Administrador reinicia la contraseña de un usuario desde el
-- panel "Usuario y Acceso" (CU05), para que la contraseña
-- temporal que le entrega no se quede sirviendo como definitiva.
--
-- Ejecuta este script UNA VEZ contra tu base blautech_bd antes
-- de reiniciar el backend (usa ddl-auto=validate, así que Spring
-- Boot no va a crear esta columna solo; hay que aplicarla a mano).
-- ============================================================

ALTER TABLE usuario
    ADD COLUMN debe_cambiar_contrasena TINYINT(1) NOT NULL DEFAULT 0 AFTER contrasena;
