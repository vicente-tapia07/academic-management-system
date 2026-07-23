-- ============================================================
-- Integrante 2 — Ubicación en tiempo real + Accesibilidad
-- Tabla propia: accessibility_poi (rampas / puntos de accesibilidad)
-- No modifica ninguna tabla de otros integrantes.
-- Se ejecuta después de 1_db_schema.sql y 2_db_mock.sql
-- (Docker corre los .sql de /database en orden alfabético)
-- ============================================================

CREATE TABLE IF NOT EXISTS accessibility_poi (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    building_id BIGINT REFERENCES building(id) ON DELETE SET NULL,
    geom GEOMETRY(POINT, 4326) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ramp_geom ON accessibility_poi USING GIST(geom);

-- ── Datos de prueba ──────────────────────────────────────────
-- Referencia (de 2_db_mock.sql):
--   building 1 = FING  | room A-101 (id=1) POINT(-70.6845 -33.4487)
--                       | room A-102 (id=2) POINT(-70.6844 -33.4488)
--   building 2 = FCI   | room B-201 (id=3) POINT(-70.6832 -33.4494)
--                       | room B-202 (id=4) POINT(-70.6831 -33.4495)

-- Rampa MUY cerca de A-101 y A-102 (~15-20m) -> ambas salas de FING
-- deberían salir como accesible=true al consultar /api/rooms/accessible?buildingId=1
INSERT INTO accessibility_poi (name, building_id, geom)
VALUES ('Rampa acceso norte FING', 1, ST_GeomFromText('POINT(-70.68448 -33.44865)', 4326));

-- Rampa deliberadamente LEJOS (~200m) de B-201 y B-202 -> las salas de FCI
-- deberían salir como accesible=false al consultar /api/rooms/accessible?buildingId=2
-- (sirve para probar el caso negativo, no solo el positivo)
INSERT INTO accessibility_poi (name, building_id, geom)
VALUES ('Rampa estacionamiento FCI', 2, ST_GeomFromText('POINT(-70.6815 -33.4494)', 4326));

-- POI suelto, sin building_id (nullable) -> prueba el ON DELETE SET NULL
-- y confirma que el campo building_id es opcional en el CRUD
INSERT INTO accessibility_poi (name, building_id, geom)
VALUES ('Rampa biblioteca central', NULL, ST_GeomFromText('POINT(-70.6850 -33.4490)', 4326));
