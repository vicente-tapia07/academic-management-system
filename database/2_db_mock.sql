-- ============================================================
-- DATOS DE DEMOSTRACIÓN ACADÉMICA
-- Historias coherentes para Laboratorios 1 y 2 (julio de 2026)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── Carreras ─────────────────────────────────────────────────
INSERT INTO career (code, name) VALUES
    ('INF', 'Ingeniería Informática'),
    ('ICI', 'Ingeniería Civil Industrial');

-- ── Catálogo focal ───────────────────────────────────────────
INSERT INTO subject (code, name, credits, career_id, active)
SELECT data.code, data.name, data.credits, c.id, true
FROM (
    VALUES
        ('CAL1', 'Cálculo 1',      4),
        ('CAL2', 'Cálculo 2',      4),
        ('PRG1', 'Programación 1', 4),
        ('PRG2', 'Programación 2', 4),
        ('BDD1', 'Base de Datos',  4),
        ('ALG1', 'Álgebra 1',      3),
        ('FIS1', 'Física 1',       4),
        ('ALG2', 'Álgebra 2',      3),
        ('FIS2', 'Física 2',       4)
) AS data(code, name, credits)
JOIN career c ON c.code = 'INF';

INSERT INTO prerequisite (subject_id, prerequisite_subject_id)
SELECT subject.id, required.id
FROM (
    VALUES
        ('CAL2', 'CAL1'),
        ('ALG2', 'ALG1'),
        ('FIS2', 'FIS1'),
        ('PRG2', 'PRG1'),
        ('BDD1', 'PRG1')
) AS data(subject_code, prerequisite_code)
JOIN subject ON subject.code = data.subject_code
JOIN subject required ON required.code = data.prerequisite_code;

-- ── Periodos académicos ──────────────────────────────────────
INSERT INTO semester
    (year, period, start_date, end_date, grade_start_date, grade_end_date, status)
VALUES
    (2024, '1S', '2024-03-04', '2024-07-26', '2024-06-17', '2024-07-26', 'CLOSED'),
    (2024, '2S', '2024-08-05', '2024-12-20', '2024-11-25', '2024-12-20', 'CLOSED'),
    (2025, '1S', '2025-03-03', '2025-07-25', '2025-06-16', '2025-07-25', 'CLOSED'),
    (2025, '2S', '2025-08-04', '2025-12-19', '2025-11-24', '2025-12-19', 'CLOSED'),
    (2026, '1S', '2026-03-02', '2026-08-14', '2026-07-13', '2026-08-14', 'IN_PROGRESS'),
    (2026, '2S', '2026-08-17', '2026-12-18', '2026-11-23', '2026-12-18', 'PLANNED');

-- ── Usuarios de demostración ─────────────────────────────────
-- Todas las claves son fixtures locales; no representan credenciales reales.
INSERT INTO usuario (rut, email, password_hash, rol) VALUES
    ('12345678-9', 'juan@usach.cl',          crypt('1234', gen_salt('bf', 10)), 'STUDENT'),
    ('98765432-1', 'maria@usach.cl',         crypt('1234', gen_salt('bf', 10)), 'STUDENT'),
    ('13579246-8', 'pedro@usach.cl',         crypt('1234', gen_salt('bf', 10)), 'STUDENT'),
    ('24681357-9', 'lucas@usach.cl',         crypt('1234', gen_salt('bf', 10)), 'STUDENT'),
    ('11111111-1', 'admin@usach.cl',         crypt('1234', gen_salt('bf', 10)), 'ADMIN'),
    ('22222222-2', 'admin2@usach.cl',        crypt('1234', gen_salt('bf', 10)), 'ADMIN'),
    ('11222333-4', 'carlos@usach.cl',        crypt('1234', gen_salt('bf', 10)), 'PROFESSOR'),
    ('55666777-8', 'ana@usach.cl',           crypt('1234', gen_salt('bf', 10)), 'PROFESSOR'),
    ('33344455-6', 'diego@usach.cl',         crypt('1234', gen_salt('bf', 10)), 'PROFESSOR'),
    ('44455566-7', 'elena.vargas@usach.cl',  crypt('1234', gen_salt('bf', 10)), 'PROFESSOR');

INSERT INTO professor (usuario_id, first_name, last_name, department)
SELECT u.id, data.first_name, data.last_name, data.department
FROM (
    VALUES
        ('carlos@usach.cl',       'Carlos', 'Ruiz',   'Informática'),
        ('ana@usach.cl',          'Ana',    'López',  'Matemáticas'),
        ('diego@usach.cl',        'Diego',  'Mora',   'Matemáticas'),
        ('elena.vargas@usach.cl', 'Elena',  'Vargas', 'Física')
) AS data(email, first_name, last_name, department)
JOIN usuario u ON u.email = data.email;

INSERT INTO student
    (usuario_id, enrollment_number, first_name, last_name, academic_status)
SELECT u.id, data.enrollment_number, data.first_name, data.last_name, 'ACTIVE'
FROM (
    VALUES
        ('juan@usach.cl',  '2024001', 'Juan',  'Pérez'),
        ('maria@usach.cl', '2024002', 'María', 'González'),
        ('pedro@usach.cl', '2024003', 'Pedro', 'Soto'),
        ('lucas@usach.cl', '2024004', 'Lucas', 'Torres')
) AS data(email, enrollment_number, first_name, last_name)
JOIN usuario u ON u.email = data.email;

-- ── Infraestructura física ───────────────────────────────────
INSERT INTO building (code, name, geom) VALUES
    ('FING', 'Facultad de Ingeniería',
        ST_GeomFromText('POLYGON((-70.6847 -33.4486, -70.6843 -33.4486, -70.6843 -33.4489, -70.6847 -33.4489, -70.6847 -33.4486))', 4326)),
    ('FCI', 'Facultad de Ciencias',
        ST_GeomFromText('POLYGON((-70.6834 -33.4493, -70.6830 -33.4493, -70.6830 -33.4496, -70.6834 -33.4496, -70.6834 -33.4493))', 4326));

INSERT INTO room (building_id, code, name, capacity, geom)
SELECT b.id, data.room_code, data.room_name, data.capacity,
       ST_GeomFromText(data.point_wkt, 4326)
FROM (
    VALUES
        ('FING', 'A-101', 'Sala 101', 35, 'POINT(-70.6845 -33.4487)'),
        ('FING', 'A-102', 'Sala 102', 30, 'POINT(-70.6844 -33.4488)'),
        ('FCI',  'B-201', 'Sala 201', 40, 'POINT(-70.6832 -33.4494)'),
        ('FCI',  'B-202', 'Sala 202', 25, 'POINT(-70.6831 -33.4495)')
) AS data(building_code, room_code, room_name, capacity, point_wkt)
JOIN building b ON b.code = data.building_code;

-- ── Secciones ────────────────────────────────────────────────
-- Horarios base por ramo:
-- CAL1 L08:15, CAL2 X09:50, ALG1 M08:15, ALG2 J09:50,
-- FIS1 L09:50, FIS2 X08:15, PRG1 M09:50, PRG2 X13:45, BDD1 V08:15.
-- Los cupos disponibles representan el total menos todas las inscripciones
-- persistidas para la sección (en este fixture no existen cancelaciones).
INSERT INTO section
    (subject_id, professor_id, semester_id, total_seats, available_seats,
     room_id, day_of_week, start_time, end_time)
SELECT sub.id, p.id, sem.id, 20, data.available_seats,
       r.id, data.day_of_week, data.start_time, data.end_time
FROM (
    VALUES
        -- 2024-1S
        ('CAL1', 'ana@usach.cl',          2024, '1S', 'FING', 'A-101', 1, '08:15'::time, '09:35'::time, 16),
        ('ALG1', 'diego@usach.cl',        2024, '1S', 'FING', 'A-102', 2, '08:15'::time, '09:35'::time, 16),
        ('FIS1', 'elena.vargas@usach.cl', 2024, '1S', 'FCI',  'B-201', 1, '09:50'::time, '11:10'::time, 17),
        -- 2024-2S
        ('CAL1', 'ana@usach.cl',          2024, '2S', 'FING', 'A-101', 1, '08:15'::time, '09:35'::time, 19),
        ('ALG1', 'diego@usach.cl',        2024, '2S', 'FING', 'A-102', 2, '08:15'::time, '09:35'::time, 18),
        ('FIS1', 'elena.vargas@usach.cl', 2024, '2S', 'FCI',  'B-201', 1, '09:50'::time, '11:10'::time, 18),
        ('CAL2', 'ana@usach.cl',          2024, '2S', 'FING', 'A-101', 3, '09:50'::time, '11:10'::time, 19),
        ('ALG2', 'diego@usach.cl',        2024, '2S', 'FING', 'A-102', 4, '09:50'::time, '11:10'::time, 19),
        ('FIS2', 'elena.vargas@usach.cl', 2024, '2S', 'FCI',  'B-201', 3, '08:15'::time, '09:35'::time, 19),
        ('PRG1', 'carlos@usach.cl',       2024, '2S', 'FING', 'A-101', 2, '09:50'::time, '11:10'::time, 18),
        -- 2025-1S
        ('CAL2', 'ana@usach.cl',          2025, '1S', 'FING', 'A-101', 3, '09:50'::time, '11:10'::time, 18),
        ('ALG2', 'diego@usach.cl',        2025, '1S', 'FING', 'A-102', 4, '09:50'::time, '11:10'::time, 19),
        ('FIS2', 'elena.vargas@usach.cl', 2025, '1S', 'FCI',  'B-201', 3, '08:15'::time, '09:35'::time, 17),
        ('PRG2', 'carlos@usach.cl',       2025, '1S', 'FING', 'A-102', 3, '13:45'::time, '15:05'::time, 19),
        ('BDD1', 'carlos@usach.cl',       2025, '1S', 'FCI',  'B-202', 5, '08:15'::time, '09:35'::time, 19),
        -- 2025-2S
        ('CAL2', 'ana@usach.cl',          2025, '2S', 'FING', 'A-101', 3, '09:50'::time, '11:10'::time, 18),
        ('ALG2', 'diego@usach.cl',        2025, '2S', 'FING', 'A-102', 4, '09:50'::time, '11:10'::time, 19),
        ('PRG2', 'carlos@usach.cl',       2025, '2S', 'FING', 'A-102', 3, '13:45'::time, '15:05'::time, 19),
        -- 2026-1S: una sección por profesor para poblar sus vistas activas
        ('CAL1', 'ana@usach.cl',          2026, '1S', 'FING', 'A-101', 1, '08:15'::time, '09:35'::time, 20),
        ('PRG1', 'carlos@usach.cl',       2026, '1S', 'FING', 'A-101', 2, '09:50'::time, '11:10'::time, 19),
        ('FIS2', 'elena.vargas@usach.cl', 2026, '1S', 'FCI',  'B-201', 3, '08:15'::time, '09:35'::time, 19),
        ('ALG2', 'diego@usach.cl',        2026, '1S', 'FING', 'A-102', 4, '09:50'::time, '11:10'::time, 19),
        ('PRG2', 'carlos@usach.cl',       2026, '1S', 'FING', 'A-102', 3, '13:45'::time, '15:05'::time, 19)
) AS data(
    subject_code, professor_email, semester_year, semester_period,
    building_code, room_code, day_of_week, start_time, end_time, available_seats
)
JOIN subject sub ON sub.code = data.subject_code
JOIN usuario professor_user ON professor_user.email = data.professor_email
JOIN professor p ON p.usuario_id = professor_user.id
JOIN semester sem
    ON sem.year = data.semester_year AND sem.period = data.semester_period
JOIN building b ON b.code = data.building_code
JOIN room r ON r.building_id = b.id AND r.code = data.room_code;

-- ── Historias académicas cerradas ────────────────────────────
-- El trigger rechaza notas en semestres CLOSED. Se desactiva dentro de esta
-- transacción únicamente para cargar el historial. Si algo falla, el ROLLBACK
-- también revierte la desactivación y el trigger permanece habilitado.
BEGIN;
ALTER TABLE grade DISABLE TRIGGER trg_check_calendario_notas;

-- 2024-1S
INSERT INTO enrollment (student_id, section_id, enrollment_date, status)
SELECT st.id, sec.id, '2024-03-04', 'COMPLETED'
FROM (
    VALUES
        ('2024001', 'CAL1'), ('2024001', 'ALG1'),
        ('2024002', 'CAL1'), ('2024002', 'ALG1'), ('2024002', 'FIS1'),
        ('2024003', 'CAL1'), ('2024003', 'ALG1'), ('2024003', 'FIS1'),
        ('2024004', 'CAL1'), ('2024004', 'ALG1'), ('2024004', 'FIS1')
) AS data(enrollment_number, subject_code)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2024 AND sem.period = '1S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id;

INSERT INTO grade (enrollment_id, value, entry_date)
SELECT e.id, data.grade, '2024-07-01'
FROM (
    VALUES
        ('2024001', 'CAL1', 5.6), ('2024001', 'ALG1', 5.2),
        ('2024002', 'CAL1', 3.2), ('2024002', 'ALG1', 3.6), ('2024002', 'FIS1', 4.6),
        ('2024003', 'CAL1', 6.2), ('2024003', 'ALG1', 5.9), ('2024003', 'FIS1', 6.0),
        ('2024004', 'CAL1', 4.1), ('2024004', 'ALG1', 3.8), ('2024004', 'FIS1', 3.5)
) AS data(enrollment_number, subject_code, grade)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2024 AND sem.period = '1S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id
JOIN enrollment e ON e.student_id = st.id AND e.section_id = sec.id;

-- 2024-2S
INSERT INTO enrollment (student_id, section_id, enrollment_date, status)
SELECT st.id, sec.id, '2024-08-05', 'COMPLETED'
FROM (
    VALUES
        ('2024001', 'FIS1'),
        ('2024002', 'CAL1'), ('2024002', 'ALG1'),
        ('2024003', 'CAL2'), ('2024003', 'ALG2'), ('2024003', 'FIS2'), ('2024003', 'PRG1'),
        ('2024004', 'ALG1'), ('2024004', 'FIS1'), ('2024004', 'PRG1')
) AS data(enrollment_number, subject_code)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2024 AND sem.period = '2S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id;

INSERT INTO grade (enrollment_id, value, entry_date)
SELECT e.id, data.grade, '2024-12-02'
FROM (
    VALUES
        ('2024001', 'FIS1', 5.0),
        ('2024002', 'CAL1', 4.3), ('2024002', 'ALG1', 4.1),
        ('2024003', 'CAL2', 5.8), ('2024003', 'ALG2', 5.5), ('2024003', 'FIS2', 5.6), ('2024003', 'PRG1', 6.3),
        ('2024004', 'ALG1', 4.0), ('2024004', 'FIS1', 4.2), ('2024004', 'PRG1', 5.0)
) AS data(enrollment_number, subject_code, grade)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2024 AND sem.period = '2S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id
JOIN enrollment e ON e.student_id = st.id AND e.section_id = sec.id;

-- 2025-1S
INSERT INTO enrollment (student_id, section_id, enrollment_date, status)
SELECT st.id, sec.id, '2025-03-03', 'COMPLETED'
FROM (
    VALUES
        ('2024001', 'FIS2'), ('2024001', 'CAL2'),
        ('2024002', 'FIS2'),
        ('2024003', 'PRG2'), ('2024003', 'BDD1'),
        ('2024004', 'CAL2'), ('2024004', 'ALG2'), ('2024004', 'FIS2')
) AS data(enrollment_number, subject_code)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2025 AND sem.period = '1S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id;

INSERT INTO grade (enrollment_id, value, entry_date)
SELECT e.id, data.grade, '2025-07-01'
FROM (
    VALUES
        ('2024001', 'FIS2', 5.1), ('2024001', 'CAL2', 4.8),
        ('2024002', 'FIS2', 3.7),
        ('2024003', 'PRG2', 6.0), ('2024003', 'BDD1', 5.7),
        ('2024004', 'CAL2', 3.9), ('2024004', 'ALG2', 4.4), ('2024004', 'FIS2', 4.0)
) AS data(enrollment_number, subject_code, grade)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2025 AND sem.period = '1S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id
JOIN enrollment e ON e.student_id = st.id AND e.section_id = sec.id;

-- 2025-2S
INSERT INTO enrollment (student_id, section_id, enrollment_date, status)
SELECT st.id, sec.id, '2025-08-04', 'COMPLETED'
FROM (
    VALUES
        ('2024001', 'ALG2'),
        ('2024002', 'CAL2'),
        ('2024004', 'CAL2'), ('2024004', 'PRG2')
) AS data(enrollment_number, subject_code)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2025 AND sem.period = '2S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id;

INSERT INTO grade (enrollment_id, value, entry_date)
SELECT e.id, data.grade, '2025-12-01'
FROM (
    VALUES
        ('2024001', 'ALG2', 5.4),
        ('2024002', 'CAL2', 4.2),
        ('2024004', 'CAL2', 4.2), ('2024004', 'PRG2', 3.6)
) AS data(enrollment_number, subject_code, grade)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2025 AND sem.period = '2S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id
JOIN enrollment e ON e.student_id = st.id AND e.section_id = sec.id;

ALTER TABLE grade ENABLE TRIGGER trg_check_calendario_notas;
COMMIT;

-- ── Inscripciones vigentes 2026-1S ───────────────────────────
INSERT INTO enrollment (student_id, section_id, enrollment_date, status)
SELECT st.id, sec.id, '2026-03-02', 'ACTIVE'
FROM (
    VALUES
        ('2024001', 'PRG1'),
        ('2024002', 'FIS2'), ('2024002', 'ALG2'),
        ('2024004', 'PRG2')
) AS data(enrollment_number, subject_code)
JOIN student st ON st.enrollment_number = data.enrollment_number
JOIN semester sem ON sem.year = 2026 AND sem.period = '1S'
JOIN subject sub ON sub.code = data.subject_code
JOIN section sec ON sec.semester_id = sem.id AND sec.subject_id = sub.id;

-- ── Datos geoespaciales ──────────────────────────────────────
INSERT INTO housing_district (name, geom) VALUES
    ('Estación Central Norte',
        ST_GeomFromText('POLYGON((-70.6900 -33.4400, -70.6750 -33.4400, -70.6750 -33.4550, -70.6900 -33.4550, -70.6900 -33.4400))', 4326)),
    ('Santiago Centro Poniente',
        ST_GeomFromText('POLYGON((-70.6750 -33.4400, -70.6550 -33.4400, -70.6550 -33.4550, -70.6750 -33.4550, -70.6750 -33.4400))', 4326));

UPDATE student st
SET home_location = ST_GeomFromText(data.point_wkt, 4326)
FROM (
    VALUES
        ('2024001', 'POINT(-70.6820 -33.4450)'),
        ('2024002', 'POINT(-70.6810 -33.4480)'),
        ('2024003', 'POINT(-70.6650 -33.4420)'),
        ('2024004', 'POINT(-70.6660 -33.4490)')
) AS data(enrollment_number, point_wkt)
WHERE st.enrollment_number = data.enrollment_number;

REFRESH MATERIALIZED VIEW mv_failure_rate;
REFRESH MATERIALIZED VIEW mv_student_density_by_building;
REFRESH MATERIALIZED VIEW mv_failure_rate_by_district;
