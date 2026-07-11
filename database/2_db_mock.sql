-- =============================================
-- DATOS DE PRUEBA — db_mock.sql
-- Ejecutar DESPUÉS de db_schema.sql
-- Contraseña de todos los usuarios: 1234
-- =============================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── Carreras ─────────────────────────────────
INSERT INTO career (code, name) VALUES ('INF', 'Ingeniería Informática');
INSERT INTO career (code, name) VALUES ('ICI', 'Ingeniería Civil Industrial');

-- ── Asignaturas ──────────────────────────────
INSERT INTO subject (code, name, credits, career_id) VALUES ('CAL1', 'Cálculo 1',      4, 1);
INSERT INTO subject (code, name, credits, career_id) VALUES ('CAL2', 'Cálculo 2',      4, 1);
INSERT INTO subject (code, name, credits, career_id) VALUES ('PRG1', 'Programación 1', 4, 1);
INSERT INTO subject (code, name, credits, career_id) VALUES ('PRG2', 'Programación 2', 4, 1);
INSERT INTO subject (code, name, credits, career_id) VALUES ('BDD1', 'Base de Datos',  4, 1);
INSERT INTO subject (code, name, credits, career_id) VALUES ('ALG1', 'Álgebra 1',      3, 1);

-- ── Prerequisitos ────────────────────────────
INSERT INTO prerequisite (subject_id, prerequisite_subject_id) VALUES (2, 1); -- CAL2 requiere CAL1
INSERT INTO prerequisite (subject_id, prerequisite_subject_id) VALUES (4, 3); -- PRG2 requiere PRG1
INSERT INTO prerequisite (subject_id, prerequisite_subject_id) VALUES (5, 3); -- BDD1 requiere PRG1

-- ── Semestres ────────────────────────────────
-- Semestre 1: CERRADO (para probar bloqueo de notas)
INSERT INTO semester (year, period, start_date, end_date, grade_start_date, grade_end_date, status)
VALUES (2024, '1S', '2024-03-01', '2024-07-31', '2024-06-01', '2024-07-31', 'CLOSED');

-- Semestre 2: EN PROGRESO con periodo de notas ACTIVO HOY
INSERT INTO semester (year, period, start_date, end_date, grade_start_date, grade_end_date, status)
VALUES (2025, '1S', '2025-03-01', '2025-07-31', '2025-01-01', '2026-12-31', 'IN_PROGRESS');

-- Semestre 3: PLANIFICADO (para probar cierre)
INSERT INTO semester (year, period, start_date, end_date, grade_start_date, grade_end_date, status)
VALUES (2026, '1S', '2026-03-01', '2026-07-31', '2026-06-01', '2026-07-31', 'PLANNED');

-- ── Usuarios ─────────────────────────────────
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('12345678-9', 'juan@usach.cl',        crypt('1234', gen_salt('bf', 10)), 'STUDENT');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('98765432-1', 'maria@usach.cl',       crypt('1234', gen_salt('bf', 10)), 'STUDENT');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('13579246-8', 'pedro@usach.cl',       crypt('1234', gen_salt('bf', 10)), 'STUDENT');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('24681357-9', 'ana.student@usach.cl', crypt('1234', gen_salt('bf', 10)), 'STUDENT');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('11111111-1', 'admin@usach.cl',       crypt('1234', gen_salt('bf', 10)), 'ADMIN');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('22222222-2', 'admin2@usach.cl',      crypt('1234', gen_salt('bf', 10)), 'ADMIN');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('11222333-4', 'carlos@usach.cl',      crypt('1234', gen_salt('bf', 10)), 'PROFESSOR');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('55666777-8', 'ana@usach.cl',         crypt('1234', gen_salt('bf', 10)), 'PROFESSOR');
INSERT INTO usuario (rut, email, password_hash, rol) VALUES ('33344455-6', 'diego@usach.cl',       crypt('1234', gen_salt('bf', 10)), 'PROFESSOR');

-- ── Profesores ───────────────────────────────
INSERT INTO professor (usuario_id, first_name, last_name, department) VALUES (7, 'Carlos', 'Ruiz',  'Informática');
INSERT INTO professor (usuario_id, first_name, last_name, department) VALUES (8, 'Ana',    'López', 'Matemáticas');
INSERT INTO professor (usuario_id, first_name, last_name, department) VALUES (9, 'Diego',  'Mora',  'Matemáticas');

-- ── Estudiantes ──────────────────────────────
-- Juan (id=1): aprueba todo en sem1 → puede CAL2, PRG2, BDD1 en sem2
INSERT INTO student (usuario_id, enrollment_number, first_name, last_name, academic_status) VALUES (1, '2024001', 'Juan',  'Pérez',    'ACTIVE');
-- María (id=2): reprueba CAL1 → NO puede CAL2 en sem2
INSERT INTO student (usuario_id, enrollment_number, first_name, last_name, academic_status) VALUES (2, '2024002', 'María', 'González', 'ACTIVE');
-- Pedro (id=3): aprueba todo → puede todo en sem2
INSERT INTO student (usuario_id, enrollment_number, first_name, last_name, academic_status) VALUES (3, '2024003', 'Pedro', 'Soto',     'ACTIVE');
-- Lucas (id=4): sin notas aún → no puede ramos con prerequisito
INSERT INTO student (usuario_id, enrollment_number, first_name, last_name, academic_status) VALUES (4, '2024004', 'Lucas', 'Torres',   'ACTIVE');

-- ── Edificios ────────────────────────────────
INSERT INTO building (code, name, geom) VALUES ('FING', 'Facultad de Ingeniería',ST_GeomFromText('POLYGON((-70.6847 -33.4486, -70.6843 -33.4486, -70.6843 -33.4489, -70.6847 -33.4489, -70.6847 -33.4486))', 4326));
INSERT INTO building (code, name, geom) VALUES ('FCI', 'Facultad de Ciencias',ST_GeomFromText('POLYGON((-70.6834 -33.4493, -70.6830 -33.4493, -70.6830 -33.4496, -70.6834 -33.4496, -70.6834 -33.4493))', 4326));

-- ── Salas ────────────────────────────────────
-- building_id=1 → Facultad de Ingeniería
INSERT INTO room (building_id, code, name, capacity, geom) VALUES (1, 'A-101', 'Sala 101', 35,ST_GeomFromText('POINT(-70.6845 -33.4487)', 4326));
INSERT INTO room (building_id, code, name, capacity, geom) VALUES (1, 'A-102', 'Sala 102', 30,ST_GeomFromText('POINT(-70.6844 -33.4488)', 4326));

-- building_id=2 → Facultad de Ciencias
INSERT INTO room (building_id, code, name, capacity, geom) VALUES (2, 'B-201', 'Sala 201', 40,ST_GeomFromText('POINT(-70.6832 -33.4494)', 4326));
INSERT INTO room (building_id, code, name, capacity, geom) VALUES (2, 'B-202', 'Sala 202', 25,ST_GeomFromText('POINT(-70.6831 -33.4495)', 4326));

-- ── Secciones semestre 1 (2024 CLOSED) ───────
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (1, 2, 1, 30, 27, 1, 1, '08:00', '09:30'); -- sec=1: CAL1, Sala 101, Lunes
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (3, 1, 1, 30, 27, 2, 1, '10:00', '11:30'); -- sec=2: PRG1, Sala 102, Lunes
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (6, 3, 1, 30, 28, 3, 2, '08:00', '09:30'); -- sec=3: ALG1, Sala 201, Martes

-- ── Secciones semestre 2 (2025 IN_PROGRESS) ──
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (1, 3, 2, 30, 28, 1, 3, '08:00', '09:30'); -- sec=4: CAL1 repitentes, Sala 101, Miércoles
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (2, 2, 2, 30, 2, 2, 3, '10:00', '11:30'); -- sec=5: CAL2, Sala 102, Miércoles
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (4, 1, 2, 30, 28, 3, 4, '10:00', '11:30'); -- sec=6: PRG2, Sala 201, Jueves
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (5, 1, 2, 30, 29, 4, 4, '14:00', '15:30'); -- sec=7: BDD1, Sala 202, Jueves
INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats, room_id, day_of_week, start_time, end_time) VALUES (6, 2, 2, 0, 0, 4, 5, '08:00', '09:30'); -- sec=8: ALG1 sin cupos, Sala 202, Viernes


-- ── Inscripciones semestre 1 ──────────────────
-- Juan: CAL1 y PRG1
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (1, 1, '2024-03-01', 'COMPLETED');
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (1, 2, '2024-03-01', 'COMPLETED');
-- María: solo CAL1
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (2, 1, '2024-03-01', 'COMPLETED');
-- Pedro: CAL1 y PRG1
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (3, 1, '2024-03-01', 'COMPLETED');
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (3, 2, '2024-03-01', 'COMPLETED');

-- ── Notas semestre 1 (trigger desactivado) ────
ALTER TABLE grade DISABLE TRIGGER trg_check_calendario_notas;

INSERT INTO grade (enrollment_id, value, entry_date) VALUES (1, 6.0, '2024-07-01'); -- Juan CAL1 ✅
INSERT INTO grade (enrollment_id, value, entry_date) VALUES (2, 5.5, '2024-07-01'); -- Juan PRG1 ✅
INSERT INTO grade (enrollment_id, value, entry_date) VALUES (3, 3.5, '2024-07-01'); -- María CAL1 ❌ REPRUEBA
INSERT INTO grade (enrollment_id, value, entry_date) VALUES (4, 4.5, '2024-07-01'); -- Pedro CAL1 ✅
INSERT INTO grade (enrollment_id, value, entry_date) VALUES (5, 5.0, '2024-07-01'); -- Pedro PRG1 ✅

ALTER TABLE grade ENABLE TRIGGER trg_check_calendario_notas;

-- ── Inscripciones semestre 2 ──────────────────
-- Juan: CAL2 y PRG2 (tiene prerequisitos ✅)
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (1, 5, '2025-03-01', 'ACTIVE');
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (1, 6, '2025-03-01', 'ACTIVE');
-- María: repite CAL1 (reprobó ❌)
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (2, 4, '2025-03-01', 'ACTIVE');
-- Pedro: CAL2 y BDD1
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (3, 5, '2025-03-01', 'ACTIVE');
INSERT INTO enrollment (student_id, section_id, enrollment_date, status) VALUES (3, 7, '2025-03-01', 'ACTIVE');