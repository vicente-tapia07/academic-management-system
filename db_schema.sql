-- =============================================
-- STORED PROCEDURE: cierre de semestre
-- =============================================
CREATE OR REPLACE PROCEDURE public.sp_cierre_semestre(IN p_semestre_id bigint)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    v_estudiante_id BIGINT;
    v_promedio NUMERIC;
BEGIN
    -- recorre todos los estudiantes que tienen inscripciones en el semestre
    FOR v_estudiante_id IN
        SELECT DISTINCT i.estudiante_id
        FROM inscripcion i
        JOIN seccion s ON i.seccion_id = s.id
        WHERE s.semestre_id = p_semestre_id
    LOOP
        -- calcula el promedio ponderado de las notas del estudiante en ese semestre
        SELECT ROUND(
            SUM(n.valor * a.creditos) / NULLIF(SUM(a.creditos), 0), 2
        )
        INTO v_promedio
        FROM nota n
        JOIN inscripcion i ON n.inscripcion_id = i.id
        JOIN seccion s ON i.seccion_id = s.id
        JOIN asignatura a ON s.asignatura_id = a.id
        WHERE i.estudiante_id = v_estudiante_id
          AND s.semestre_id = p_semestre_id;

        -- si el promedio es menor a 4.0 cambia estado del estudiante a BLOQUEADO
        IF v_promedio IS NOT NULL AND v_promedio < 4.0 THEN
            UPDATE estudiante
            SET estado_academico = 'BLOQUEADO'
            WHERE id = v_estudiante_id;
        END IF;
    END LOOP;

    -- cierra el semestre cambiando su estado a CERRADO
    UPDATE semestre
    SET estado = 'CERRADO'
    WHERE id = p_semestre_id;

    RAISE NOTICE 'Semestre % cerrado correctamente', p_semestre_id;
END;
$procedure$;

-- =============================================
-- FUNCIÓN: check calendario notas
-- =============================================
CREATE OR REPLACE FUNCTION fn_check_calendario_notas()
RETURNS TRIGGER AS $$
DECLARE
    v_grade_start DATE;
    v_grade_end DATE;
BEGIN
    SELECT s.grade_start_date, s.grade_end_date
    INTO v_grade_start, v_grade_end
    FROM semester s
    INNER JOIN section sec ON sec.semester_id = s.id
    WHERE sec.id = NEW.section_id;

    IF CURRENT_DATE < v_grade_start OR CURRENT_DATE > v_grade_end THEN
        RAISE EXCEPTION 'No se pueden ingresar notas fuera del calendario académico';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- TRIGGER: check calendario notas
-- =============================================
CREATE TRIGGER trg_check_calendario_notas
BEFORE INSERT OR UPDATE ON grade
FOR EACH ROW
EXECUTE FUNCTION fn_check_calendario_notas();

-- =============================================
-- ÍNDICES
-- =============================================
CREATE INDEX idx_subject_code ON subject(code);
CREATE INDEX idx_section_semester ON section(semester_id);