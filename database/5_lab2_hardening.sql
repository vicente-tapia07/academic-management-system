-- Migración idempotente para instalaciones existentes de Lab 2.
-- En una base nueva, 1_db_schema.sql ya contiene estas reglas; este archivo
-- también permite actualizar un volumen persistente sin eliminar sus datos.

BEGIN;

CREATE TABLE IF NOT EXISTS audit (
    id BIGSERIAL PRIMARY KEY,
    affected_table VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    usuario_rut VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    operation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_data JSONB,
    new_data JSONB
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_semester_year_period') THEN
        ALTER TABLE semester ADD CONSTRAINT uq_semester_year_period UNIQUE (year, period);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_semester_dates') THEN
        ALTER TABLE semester ADD CONSTRAINT chk_semester_dates CHECK (
            end_date >= start_date
            AND grade_start_date >= start_date
            AND grade_end_date >= grade_start_date
            AND grade_end_date <= end_date
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_professor_usuario') THEN
        ALTER TABLE professor ADD CONSTRAINT uq_professor_usuario UNIQUE (usuario_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_student_usuario') THEN
        ALTER TABLE student ADD CONSTRAINT uq_student_usuario UNIQUE (usuario_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_grade_enrollment') THEN
        ALTER TABLE grade ADD CONSTRAINT uq_grade_enrollment UNIQUE (enrollment_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_room_capacity') THEN
        ALTER TABLE room ADD CONSTRAINT chk_room_capacity CHECK (capacity > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_subject_credits') THEN
        ALTER TABLE subject ADD CONSTRAINT chk_subject_credits CHECK (credits > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_section_seats') THEN
        ALTER TABLE section ADD CONSTRAINT chk_section_seats
            CHECK (total_seats > 0 AND available_seats BETWEEN 0 AND total_seats);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_section_schedule') THEN
        ALTER TABLE section ADD CONSTRAINT chk_section_schedule CHECK (end_time > start_time);
    END IF;
END;
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_one_in_progress_semester
    ON semester ((status)) WHERE status = 'IN_PROGRESS';

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
    IF v_status <> 'IN_PROGRESS' THEN
        RAISE EXCEPTION 'Solo se pueden ingresar notas en el semestre en curso';
    END IF;
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
    WHERE e.student_id = p_student_id
      AND sec.subject_id = v_subject_id
      AND g.value >= 4.0
      AND e.status = 'COMPLETED';
    IF v_already_approved > 0 THEN
        RAISE EXCEPTION 'El estudiante ya aprobó esta asignatura';
    END IF;

    SELECT COUNT(*) INTO v_already_enrolled
    FROM enrollment e
    JOIN section sec ON sec.id = e.section_id
    WHERE e.student_id = p_student_id
      AND sec.subject_id = v_subject_id
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

    UPDATE section
    SET available_seats = available_seats - 1
    WHERE id = p_section_id;
END;
$$;

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

    FOR v_student_id IN
        SELECT DISTINCT e.student_id
        FROM enrollment e
        JOIN section sec ON sec.id = e.section_id
        WHERE sec.semester_id = p_semester_id
    LOOP
        SELECT ROUND(SUM(g.value * sub.credits) / NULLIF(SUM(sub.credits), 0), 2)
        INTO v_avg
        FROM grade g
        JOIN enrollment e ON g.enrollment_id = e.id
        JOIN section sec ON e.section_id = sec.id
        JOIN subject sub ON sec.subject_id = sub.id
        WHERE e.student_id = v_student_id
          AND sec.semester_id = p_semester_id;

        IF v_avg IS NOT NULL AND v_avg < 4.0 THEN
            UPDATE student SET academic_status = 'BLOCKED' WHERE id = v_student_id;
        END IF;
    END LOOP;

    UPDATE enrollment e
    SET status = 'COMPLETED'
    FROM section sec
    WHERE sec.id = e.section_id
      AND sec.semester_id = p_semester_id
      AND e.status = 'ACTIVE';

    UPDATE semester SET status = 'CLOSED' WHERE id = p_semester_id;
END;
$$;

-- Corrige el fixture histórico que dejaba PRG1 fuera de los bloques oficiales.
UPDATE section sec
SET day_of_week = 2,
    start_time = '09:50:00',
    end_time = '11:10:00'
FROM subject sub, semester sem
WHERE sec.subject_id = sub.id
  AND sec.semester_id = sem.id
  AND sub.code = 'PRG1'
  AND sem.year = 2026
  AND sem.period = '1S';

COMMIT;
