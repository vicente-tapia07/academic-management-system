-- ============================================================
-- Integrante 2 — Ubicación en tiempo real
-- Sección de prueba SIEMPRE ACTIVA para poder probar
-- POST /api/location/nearest-room sin depender de que la hora
-- real coincida con los horarios fijos del mock (2_db_mock.sql).
--
-- Clave: el día y la ventana horaria se calculan con funciones
-- SQL (EXTRACT(DOW FROM CURRENT_TIMESTAMP), CURRENT_TIME) en vez
-- de valores fijos. Así, cada vez que el contenedor se inicializa
-- desde cero (docker-compose down -v && up --build), esta fila
-- se recalcula sola para "ahora mismo" -> no hay que editar nada
-- a mano ni volver a hacer esto en el futuro.
--
-- Usa: student_id=1 (Juan, ya tiene usuario/rol STUDENT en el mock),
-- room_id=1 (Sala A-101, building FING, POINT(-70.6845 -33.4487)).
-- Ventana de ±2 horas alrededor del momento de inicialización.
-- ============================================================

DO $$
DECLARE
    new_section_id BIGINT;
BEGIN
    INSERT INTO section (subject_id, professor_id, semester_id, total_seats, available_seats,
                          room_id, day_of_week, start_time, end_time)
    VALUES (1, 2, 1, 30, 27, 1,
            EXTRACT(DOW FROM CURRENT_TIMESTAMP)::smallint,
            (CURRENT_TIME - INTERVAL '2 hours')::time,
            (CURRENT_TIME + INTERVAL '2 hours')::time)
    RETURNING id INTO new_section_id;

    INSERT INTO enrollment (student_id, section_id, enrollment_date, status)
    VALUES (1, new_section_id, CURRENT_DATE, 'ACTIVE');
END $$;
