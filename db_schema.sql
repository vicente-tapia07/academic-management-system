-- TABLAS MÓDULO ACADÉMICO 
CREATE TABLE IF NOT EXISTS public.career (
    id bigint NOT NULL DEFAULT nextval('career_id_seq'::regclass),
    code character varying(20) NOT NULL,
    name character varying(100) NOT NULL,
    CONSTRAINT career_pkey PRIMARY KEY (id),
    CONSTRAINT career_code_key UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS public.semester (
    id bigint NOT NULL DEFAULT nextval('semester_id_seq'::regclass),
    year integer NOT NULL,
    period character varying(10) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    grade_start_date date NOT NULL,
    grade_end_date date NOT NULL,
    status character varying(20) DEFAULT 'PLANNED',
    CONSTRAINT semester_pkey PRIMARY KEY (id),
    CONSTRAINT semester_status_check CHECK (status = ANY (ARRAY['PLANNED','IN_PROGRESS','CLOSED']))
);

CREATE TABLE IF NOT EXISTS public.subject (
    id bigint NOT NULL DEFAULT nextval('subject_id_seq'::regclass),
    code character varying(20) NOT NULL,
    name character varying(100) NOT NULL,
    credits integer NOT NULL,
    career_id bigint,
    CONSTRAINT subject_pkey PRIMARY KEY (id),
    CONSTRAINT subject_code_key UNIQUE (code),
    CONSTRAINT subject_career_id_fkey FOREIGN KEY (career_id)
        REFERENCES public.career (id)
);

CREATE TABLE IF NOT EXISTS public.prerequisite (
    subject_id bigint NOT NULL,
    prerequisite_subject_id bigint NOT NULL,
    CONSTRAINT prerequisite_pkey PRIMARY KEY (subject_id, prerequisite_subject_id),
    CONSTRAINT prerequisite_subject_id_fkey FOREIGN KEY (subject_id)
        REFERENCES public.subject (id),
    CONSTRAINT prerequisite_prerequisite_subject_id_fkey FOREIGN KEY (prerequisite_subject_id)
        REFERENCES public.subject (id)
);

CREATE TABLE IF NOT EXISTS public.section (
    id bigint NOT NULL DEFAULT nextval('section_id_seq'::regclass),
    subject_id bigint,
    professor_id bigint, -- FK a users, creada por Integrante 1
    semester_id bigint,
    total_seats integer NOT NULL,
    available_seats integer NOT NULL,
    CONSTRAINT section_pkey PRIMARY KEY (id),
    CONSTRAINT section_subject_id_fkey FOREIGN KEY (subject_id)
        REFERENCES public.subject (id),
    CONSTRAINT section_semester_id_fkey FOREIGN KEY (semester_id)
        REFERENCES public.semester (id)
);

-- =============================================
-- ÍNDICES
-- =============================================
CREATE INDEX IF NOT EXISTS idx_subject_code ON public.subject(code);
CREATE INDEX IF NOT EXISTS idx_section_semester ON public.section(semester_id);

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
BEFORE INSERT OR UPDATE ON public.grade
FOR EACH ROW
EXECUTE FUNCTION public.fn_check_calendario_notas();

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
    FOR v_estudiante_id IN
        SELECT DISTINCT i.estudiante_id
        FROM inscripcion i
        JOIN seccion s ON i.seccion_id = s.id
        WHERE s.semestre_id = p_semestre_id
    LOOP
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

        IF v_promedio IS NOT NULL AND v_promedio < 4.0 THEN
            UPDATE estudiante
            SET estado_academico = 'BLOQUEADO'
            WHERE id = v_estudiante_id;
        END IF;
    END LOOP;

    UPDATE semestre
    SET estado = 'CERRADO'
    WHERE id = p_semestre_id;

    RAISE NOTICE 'Semestre % cerrado correctamente', p_semestre_id;
END;
$procedure$;