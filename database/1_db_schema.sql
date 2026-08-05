-- =============================================
-- SCHEMA — db_schema.sql
-- =============================================

-- Extensión para BCrypt
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- Activar PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;
-- =============================================
-- TABLAS
-- =============================================

CREATE TABLE IF NOT EXISTS career (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS building (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    geom GEOMETRY(POLYGON, 4326) NOT NULL
);

CREATE TABLE IF NOT EXISTS room (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL CONSTRAINT chk_room_capacity CHECK (capacity > 0),
    geom GEOMETRY(POINT, 4326) NOT NULL,
    UNIQUE(building_id, code)
);

CREATE TABLE IF NOT EXISTS semester (
    id BIGSERIAL PRIMARY KEY,
    year INTEGER NOT NULL,
    period VARCHAR(10) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    grade_start_date DATE NOT NULL,
    grade_end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PLANNED'
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'CLOSED')),
    CONSTRAINT uq_semester_year_period UNIQUE (year, period),
    CONSTRAINT chk_semester_dates CHECK (
        end_date >= start_date
        AND grade_start_date >= start_date
        AND grade_end_date >= grade_start_date
        AND grade_end_date <= end_date
    )
);

CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    rut VARCHAR(12) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('STUDENT', 'PROFESSOR', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS subject (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    credits INTEGER NOT NULL CONSTRAINT chk_subject_credits CHECK (credits > 0),
    career_id BIGINT REFERENCES career(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS professor (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL CONSTRAINT uq_professor_usuario UNIQUE REFERENCES usuario(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS section (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT REFERENCES subject(id),
    professor_id BIGINT REFERENCES professor(id),
    semester_id BIGINT REFERENCES semester(id),
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL
);

ALTER TABLE section ADD COLUMN room_id BIGINT NOT NULL REFERENCES room(id);
ALTER TABLE section ADD COLUMN day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6);
ALTER TABLE section ADD COLUMN start_time TIME NOT NULL;
ALTER TABLE section ADD COLUMN end_time TIME NOT NULL;
ALTER TABLE section ADD CONSTRAINT chk_section_seats
    CHECK (total_seats > 0 AND available_seats BETWEEN 0 AND total_seats);
ALTER TABLE section ADD CONSTRAINT chk_section_schedule
    CHECK (end_time > start_time);

CREATE TABLE IF NOT EXISTS prerequisite (
    subject_id BIGINT NOT NULL REFERENCES subject(id),
    prerequisite_subject_id BIGINT NOT NULL REFERENCES subject(id),
    PRIMARY KEY (subject_id, prerequisite_subject_id)
);

CREATE TABLE IF NOT EXISTS student (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL CONSTRAINT uq_student_usuario UNIQUE REFERENCES usuario(id),
    enrollment_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    academic_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (academic_status IN ('ACTIVE', 'BLOCKED', 'GRADUATED'))
);

CREATE TABLE IF NOT EXISTS enrollment (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    section_id BIGINT NOT NULL REFERENCES section(id),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED')),
    UNIQUE(student_id, section_id)
);

CREATE TABLE IF NOT EXISTS grade (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL REFERENCES enrollment(id),
    value NUMERIC(4,2) NOT NULL CHECK (value >= 1.0 AND value <= 7.0),
    entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT uq_grade_enrollment UNIQUE (enrollment_id)
);

CREATE TABLE IF NOT EXISTS audit (
    id BIGSERIAL PRIMARY KEY,
    affected_table VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    usuario_rut VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    operation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_data JSONB,
    new_data JSONB
);

-- =============================================
-- ÍNDICES
-- =============================================

CREATE INDEX IF NOT EXISTS idx_usuario_rut ON usuario(rut);
CREATE INDEX IF NOT EXISTS idx_student_enrollment ON student(enrollment_number);
CREATE INDEX IF NOT EXISTS idx_subject_code ON subject(code);
CREATE INDEX IF NOT EXISTS idx_building_geom ON building USING GIST(geom);
CREATE INDEX IF NOT EXISTS idx_room_geom ON room USING GIST(geom);
CREATE UNIQUE INDEX IF NOT EXISTS uq_one_in_progress_semester
    ON semester ((status)) WHERE status = 'IN_PROGRESS';

-- =============================================
-- TRIGGER 1: PRERREQUISITOS
-- =============================================

CREATE OR REPLACE FUNCTION fn_check_prerequisites()
RETURNS TRIGGER AS $$
DECLARE
    v_not_approved INTEGER;
    v_subject_id BIGINT;
BEGIN
    -- Obtener la asignatura de la sección que se quiere inscribir
    SELECT subject_id INTO v_subject_id
    FROM section
    WHERE id = NEW.section_id;

    -- Contar prerequisitos no aprobados
    SELECT COUNT(*)
    INTO v_not_approved
    FROM prerequisite p
    WHERE p.subject_id = v_subject_id
    AND NOT EXISTS (
        SELECT 1
        FROM enrollment e
        JOIN grade g ON g.enrollment_id = e.id
        JOIN section sec ON sec.id = e.section_id
        WHERE e.student_id = NEW.student_id
        AND sec.subject_id = p.prerequisite_subject_id
        AND g.value >= 4.0
        AND e.status = 'COMPLETED'
    );

    -- Si hay al menos uno no aprobado, bloquear
    IF v_not_approved > 0 THEN
        RAISE EXCEPTION 'El estudiante no ha aprobado todos los prerequisitos necesarios';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_check_prerequisites
BEFORE INSERT ON enrollment
FOR EACH ROW
EXECUTE FUNCTION fn_check_prerequisites();

-- =============================================
-- TRIGGER 2: CALENDARIO DE NOTAS
-- =============================================

CREATE OR REPLACE FUNCTION fn_check_calendario_notas()
RETURNS TRIGGER AS $$
DECLARE
    v_start DATE;
    v_end DATE;
    v_status VARCHAR;
BEGIN
    SELECT s.grade_start_date, s.grade_end_date, s.status
    INTO v_start, v_end, v_status
    FROM semester s
    JOIN section sec ON sec.semester_id = s.id
    JOIN enrollment e ON e.section_id = sec.id
    WHERE e.id = NEW.enrollment_id;

    IF v_status IS NULL THEN
        RAISE EXCEPTION 'La inscripción no pertenece a un semestre válido';
    END IF;

    -- Las notas solo se modifican durante el semestre vigente.
    IF v_status <> 'IN_PROGRESS' THEN
        RAISE EXCEPTION 'Solo se pueden ingresar notas en el semestre en curso';
    END IF;

    -- Bloquear si está fuera del calendario
    IF NEW.entry_date < v_start OR NEW.entry_date > v_end THEN
        RAISE EXCEPTION 'Fuera del calendario académico de notas';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_check_calendario_notas
BEFORE INSERT OR UPDATE ON grade
FOR EACH ROW
EXECUTE FUNCTION fn_check_calendario_notas();

-- Auditoría transversal: se ejecuta aunque la nota se registre desde una ruta
-- administrativa distinta de /api/professors/grade.
CREATE OR REPLACE FUNCTION fn_audit_grade_changes()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit (
        affected_table, operation, usuario_rut, operation_date, old_data, new_data
    ) VALUES (
        'grade', TG_OP, 'SYSTEM', CURRENT_TIMESTAMP,
        CASE WHEN TG_OP = 'UPDATE' THEN to_jsonb(OLD) ELSE NULL END,
        to_jsonb(NEW)
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_audit_grade_changes
AFTER INSERT OR UPDATE ON grade
FOR EACH ROW
EXECUTE FUNCTION fn_audit_grade_changes();

-- =============================================
-- STORED PROCEDURE 1: CIERRE DE SEMESTRE
-- =============================================

CREATE OR REPLACE PROCEDURE sp_close_semester(p_semester_id BIGINT)
LANGUAGE plpgsql
AS $$
DECLARE
    v_student_id BIGINT;
    v_avg NUMERIC;
    v_status VARCHAR;
BEGIN
    SELECT status INTO v_status FROM semester WHERE id = p_semester_id FOR UPDATE;
    IF v_status IS NULL THEN
        RAISE EXCEPTION 'El semestre no existe';
    END IF;
    IF v_status <> 'IN_PROGRESS' THEN
        RAISE EXCEPTION 'Solo se puede cerrar un semestre en curso';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM enrollment e
        JOIN section sec ON sec.id = e.section_id
        LEFT JOIN grade g ON g.enrollment_id = e.id
        WHERE sec.semester_id = p_semester_id
          AND e.status = 'ACTIVE'
          AND g.id IS NULL
    ) THEN
        RAISE EXCEPTION 'No se puede cerrar el semestre: existen inscripciones activas sin nota';
    END IF;

    -- Por cada estudiante del semestre
    FOR v_student_id IN
        SELECT DISTINCT e.student_id
        FROM enrollment e
        JOIN section sec ON e.section_id = sec.id
        WHERE sec.semester_id = p_semester_id
    LOOP
        -- Calcular promedio ponderado
        SELECT ROUND(SUM(g.value * sub.credits) / NULLIF(SUM(sub.credits), 0), 2)
        INTO v_avg
        FROM grade g
        JOIN enrollment e ON g.enrollment_id = e.id
        JOIN section sec ON e.section_id = sec.id
        JOIN subject sub ON sec.subject_id = sub.id
        WHERE e.student_id = v_student_id
        AND sec.semester_id = p_semester_id;

        -- Bloquear si reprobó
        IF v_avg IS NOT NULL AND v_avg < 4.0 THEN
            UPDATE student SET academic_status = 'BLOCKED'
            WHERE id = v_student_id;
        END IF;
    END LOOP;

    UPDATE enrollment e
    SET status = 'COMPLETED'
    FROM section sec
    WHERE sec.id = e.section_id
      AND sec.semester_id = p_semester_id
      AND e.status = 'ACTIVE';

    -- Cerrar el semestre
    UPDATE semester SET status = 'CLOSED'
    WHERE id = p_semester_id;
END;
$$;

-- =============================================
-- STORED PROCEDURE 2: INSCRIPCIÓN SEGURA
-- =============================================

CREATE OR REPLACE PROCEDURE sp_enroll_student(
    p_student_id BIGINT,
    p_section_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_seats INT;
    v_status VARCHAR;
    v_subject_id BIGINT;
    v_semester_status VARCHAR;
    v_already_approved INT;
    v_already_enrolled INT;
    v_cancelled_enrollment_id BIGINT;
BEGIN
    SELECT academic_status INTO v_status FROM student WHERE id = p_student_id;
    IF v_status IS NULL THEN
        RAISE EXCEPTION 'El estudiante no existe';
    END IF;
    IF v_status <> 'ACTIVE' THEN
        RAISE EXCEPTION 'El estudiante no está activo académicamente y no puede inscribir asignaturas';
    END IF;

    SELECT sec.subject_id, sec.available_seats, sem.status
    INTO v_subject_id, v_seats, v_semester_status
    FROM section sec
    JOIN semester sem ON sem.id = sec.semester_id
    WHERE sec.id = p_section_id
    FOR UPDATE OF sec;

    IF v_subject_id IS NULL THEN
        RAISE EXCEPTION 'La sección no existe';
    END IF;
    IF v_semester_status <> 'IN_PROGRESS' THEN
        RAISE EXCEPTION 'Solo se permiten inscripciones en el semestre en curso';
    END IF;

    SELECT COUNT(*) INTO v_already_approved
    FROM enrollment e
    JOIN grade g ON g.enrollment_id = e.id
    JOIN section sec ON sec.id = e.section_id
    WHERE e.student_id = p_student_id AND sec.subject_id = v_subject_id
        AND g.value >= 4.0 AND e.status = 'COMPLETED';
    IF v_already_approved > 0 THEN
        RAISE EXCEPTION 'El estudiante ya aprobó esta asignatura';
    END IF;

    SELECT COUNT(*) INTO v_already_enrolled
    FROM enrollment e
    JOIN section sec ON sec.id = e.section_id
    WHERE e.student_id = p_student_id AND sec.subject_id = v_subject_id
        AND e.status = 'ACTIVE';
    IF v_already_enrolled > 0 THEN
        RAISE EXCEPTION 'El estudiante ya está cursando esta asignatura en otra sección';
    END IF;

    IF v_seats <= 0 THEN
        RAISE EXCEPTION 'La sección no tiene cupos disponibles';
    END IF;

    SELECT id INTO v_cancelled_enrollment_id
    FROM enrollment
    WHERE student_id = p_student_id
      AND section_id = p_section_id
      AND status = 'CANCELLED';

    IF v_cancelled_enrollment_id IS NOT NULL THEN
        UPDATE enrollment
        SET status = 'ACTIVE', enrollment_date = CURRENT_DATE
        WHERE id = v_cancelled_enrollment_id;
    ELSE
        INSERT INTO enrollment (student_id, section_id)
        VALUES (p_student_id, p_section_id);
    END IF;

    UPDATE section SET available_seats = available_seats - 1 WHERE id = p_section_id;
END;
$$;

-- =============================================
-- VISTA MATERIALIZADA: TASA DE REPROBACIÓN
-- =============================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_failure_rate AS
SELECT
    sub.id              AS subject_id,
    sub.code            AS subject_code,
    sub.name            AS subject_name,
    COUNT(g.id)         AS total_grades,
    SUM(CASE WHEN g.value < 4.0 THEN 1 ELSE 0 END) AS failed_grades,
    CASE
        WHEN COUNT(g.id) = 0 THEN 0.0
        ELSE ROUND(
            SUM(CASE WHEN g.value < 4.0 THEN 1 ELSE 0 END)::NUMERIC
            / COUNT(g.id) * 100, 2)
    END AS failure_percentage
FROM subject sub
LEFT JOIN section sec ON sub.id = sec.subject_id
LEFT JOIN enrollment e ON sec.id = e.section_id
LEFT JOIN grade g ON e.id = g.enrollment_id
GROUP BY sub.id, sub.code, sub.name;

-- =============================================
-- INTEGRANTE 4: VISTAS MATERIALIZADAS Y ZONIFICACIÓN
-- =============================================

-- 1. Tabla de Distritos de Vivienda (Polígonos)
CREATE TABLE IF NOT EXISTS housing_district (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    geom GEOMETRY(POLYGON, 4326) NOT NULL
);

-- 2. Modificación a la tabla de estudiantes (Ubicación de residencia)
ALTER TABLE student ADD COLUMN IF NOT EXISTS home_location GEOMETRY(POINT, 4326);

-- 3. Índices Espaciales GIST (Obligatorios por enunciado)
CREATE INDEX IF NOT EXISTS idx_district_geom ON housing_district USING GIST(geom);
CREATE INDEX IF NOT EXISTS idx_student_home_geom ON student USING GIST(home_location);

-- =============================================
-- VISTA MATERIALIZADA 1: DENSIDAD ESTUDIANTIL POR EDIFICIO
-- =============================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_student_density_by_building AS
SELECT 
    b.id AS building_id,
    b.code AS building_code,
    b.name AS building_name,
    ST_AsGeoJSON(b.geom) AS geom_json,
    COUNT(DISTINCT e.student_id) AS student_count
FROM building b
JOIN room r ON r.building_id = b.id
JOIN section sec ON sec.room_id = r.id
JOIN enrollment e ON e.section_id = sec.id AND e.status = 'ACTIVE'
GROUP BY b.id, b.code, b.name, b.geom;

-- =============================================
-- VISTA MATERIALIZADA 2: REPROBACIÓN POR DISTRITO DE VIVIENDA
-- =============================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_failure_rate_by_district AS
SELECT 
    hd.id AS district_id,
    hd.name AS district_name,
    ST_AsGeoJSON(hd.geom) AS geom_json,
    sub.id AS subject_id,
    sub.code AS subject_code,
    sub.name AS subject_name,
    COUNT(g.id) AS total_grades,
    SUM(CASE WHEN g.value < 4.0 THEN 1 ELSE 0 END) AS failed_grades,
    CASE 
        WHEN COUNT(g.id) = 0 THEN 0.0
        ELSE ROUND(SUM(CASE WHEN g.value < 4.0 THEN 1 ELSE 0 END)::NUMERIC / COUNT(g.id) * 100, 2)
    END AS failure_percentage
FROM housing_district hd
JOIN student st ON ST_Contains(hd.geom, st.home_location)
JOIN enrollment e ON e.student_id = st.id
JOIN section sec ON sec.id = e.section_id
JOIN subject sub ON sub.id = sec.subject_id
JOIN grade g ON g.enrollment_id = e.id
GROUP BY hd.id, hd.name, hd.geom, sub.id, sub.code, sub.name;

ALTER TABLE subject ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;
