#!/usr/bin/env python3
"""Auditoría destructiva de la API completa.

Debe ejecutarse únicamente contra una base de datos desechable inicializada con
los scripts de ``database/``. Cubre cada operación publicada en OpenAPI, roles,
casos inválidos y las transiciones académicas más importantes.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from typing import Any, Iterable


BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:19091").rstrip("/")


@dataclass
class Result:
    name: str
    ok: bool
    status: int
    expected: set[int]
    body: Any


results: list[Result] = []
covered: set[tuple[str, str]] = set()


def call(
    name: str,
    method: str,
    path: str,
    expected: int | Iterable[int],
    *,
    token: str | None = None,
    body: Any = None,
    operation: str | None = None,
) -> tuple[int, Any]:
    expected_codes = {expected} if isinstance(expected, int) else set(expected)
    headers = {"Accept": "application/json"}
    data = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode("utf-8")
    request = urllib.request.Request(
        f"{BASE_URL}{path}", data=data, headers=headers, method=method.upper()
    )
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            status = response.status
            raw = response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        status = error.code
        raw = error.read().decode("utf-8")
    except Exception as error:  # pragma: no cover - diagnóstico de entorno
        status = 0
        raw = str(error)

    try:
        parsed: Any = json.loads(raw) if raw else None
    except json.JSONDecodeError:
        parsed = raw

    ok = status in expected_codes
    results.append(Result(name, ok, status, expected_codes, parsed))
    if operation:
        covered.add((method.lower(), operation))
    return status, parsed


def require(condition: bool, name: str, detail: Any) -> None:
    results.append(Result(name, condition, 200 if condition else 0, {200}, detail))


def login(email: str, password: str, expected: int = 200) -> str | None:
    _, payload = call(
        f"login {email}",
        "POST",
        "/api/auth/login",
        expected,
        body={"email": email, "password": password},
        operation="/api/auth/login",
    )
    return payload.get("token") if expected == 200 and isinstance(payload, dict) else None


def find(items: list[dict[str, Any]], field: str, value: Any) -> dict[str, Any]:
    return next(item for item in items if item.get(field) == value)


def security_sweep() -> None:
    _, spec = call("obtener OpenAPI", "GET", "/v3/api-docs", 200)
    for path, methods in spec["paths"].items():
        if path == "/api/auth/login":
            continue
        concrete = path
        for parameter in ("id", "studentId", "sectionId", "professorId"):
            concrete = concrete.replace("{" + parameter + "}", "1")
        for method, operation in methods.items():
            if method not in {"get", "post", "put", "patch", "delete"}:
                continue
            query: list[tuple[str, str]] = []
            for parameter in operation.get("parameters", []):
                if parameter.get("in") == "query" and parameter.get("required"):
                    value = "-33.45" if parameter["name"] == "lat" else "-70.68"
                    if parameter["name"] in {"subjectId", "professorRut"}:
                        value = "1" if parameter["name"] == "subjectId" else "11222333-4"
                    query.append((parameter["name"], value))
            if query:
                concrete += "?" + urllib.parse.urlencode(query)
            body = {} if operation.get("requestBody") else None
            call(
                f"sin JWT {method.upper()} {path}",
                method,
                concrete,
                401,
                body=body,
            )


def main() -> int:
    # Autenticación y rechazo de credenciales/cuerpos inválidos.
    admin = login("admin@usach.cl", "1234")
    student = login("juan@usach.cl", "1234")
    professor = login("carlos@usach.cl", "1234")
    if not all((admin, student, professor)):
        print("No fue posible obtener los tokens base", file=sys.stderr)
        return 1
    login("juan@usach.cl", "incorrecta", 401)
    call(
        "login sin password",
        "POST",
        "/api/auth/login",
        400,
        body={"email": "juan@usach.cl"},
        operation="/api/auth/login",
    )
    security_sweep()

    # Lecturas base y reportes.
    _, careers = call("listar carreras", "GET", "/api/careers", 200, token=admin,
                      operation="/api/careers")
    _, subjects = call("listar asignaturas", "GET", "/api/subjects", 200, token=admin,
                       operation="/api/subjects")
    _, semesters = call("listar semestres", "GET", "/api/semesters", 200, token=admin,
                        operation="/api/semesters")
    _, buildings = call("listar edificios", "GET", "/api/buildings", 200, token=admin,
                        operation="/api/buildings")
    _, rooms = call("listar salas", "GET", "/api/rooms", 200, token=admin,
                    operation="/api/rooms")
    _, professors = call("listar profesores", "GET", "/api/professors", 200, token=admin,
                         operation="/api/professors")
    _, students = call("listar estudiantes", "GET", "/api/students", 200, token=admin,
                       operation="/api/students")
    _, sections = call("listar secciones", "GET", "/api/sections", 200, token=admin,
                       operation="/api/sections")
    call("listar inscripciones", "GET", "/api/enrollments", 200, token=admin,
         operation="/api/enrollments")
    call("listar notas", "GET", "/api/grades", 200, token=professor,
         operation="/api/grades")
    call("salas accesibles", "GET", "/api/rooms/accessible", 200, token=student,
         operation="/api/rooms/accessible")
    call("densidad", "GET", "/api/reports/density-heatmap", 200, token=admin,
         operation="/api/reports/density-heatmap")
    call("reprobación por distrito", "GET", "/api/reports/failure-by-district", 200,
         token=admin, operation="/api/reports/failure-by-district")
    call("refrescar reportes", "POST", "/api/reports/refresh", 200, token=admin,
         operation="/api/reports/refresh")
    call("reporte profesor", "GET", "/api/professors/reports", 200, token=professor,
         operation="/api/professors/reports")
    call("alumno no refresca reportes", "POST", "/api/reports/refresh", 403, token=student)

    current_semester = find(semesters, "status", "IN_PROGRESS")
    career = careers[0]
    seed_room = rooms[0]

    # Usuarios y administradores.
    _, generic_user = call(
        "crear usuario",
        "POST",
        "/api/users",
        200,
        token=admin,
        body={"name": "70000000-1", "email": "audit.user@usach.cl", "password": "Audit123!", "role": "STUDENT"},
        operation="/api/users",
    )
    generic_user_id = generic_user["id"]
    call("obtener usuario", "GET", f"/api/users/{generic_user_id}", 200, token=admin,
         operation="/api/users/{id}")
    require("password" not in generic_user, "password no expuesta en usuario", generic_user)
    login("audit.user@usach.cl", "Audit123!")
    call("usuario con rol inválido", "POST", "/api/users", 400, token=admin,
         body={"name": "x", "email": "bad-role@usach.cl", "password": "x", "role": "ROOT"})

    _, new_admin = call(
        "crear administrador", "POST", "/api/admins", 200, token=admin,
        body={"name": "70000000-2", "email": "audit.admin@usach.cl", "password": "Audit123!", "role": "ADMIN"},
        operation="/api/admins",
    )
    new_admin_id = new_admin["id"]
    call("listar administradores", "GET", "/api/admins", 200, token=admin,
         operation="/api/admins")
    call("obtener administrador", "GET", f"/api/admins/{new_admin_id}", 200, token=admin,
         operation="/api/admins/{id}")
    call(
        "actualizar administrador", "PUT", f"/api/admins/{new_admin_id}", 200, token=admin,
        body={"name": "70000000-3", "email": "audit.admin2@usach.cl", "password": "", "role": "ADMIN"},
        operation="/api/admins/{id}",
    )
    login("audit.admin2@usach.cl", "Audit123!")
    _, delete_admin = call(
        "crear administrador eliminable", "POST", "/api/admins", 200, token=admin,
        body={"name": "70000000-4", "email": "audit.admin.delete@usach.cl", "password": "Audit123!", "role": "ADMIN"},
    )
    call("eliminar administrador", "DELETE", f"/api/admins/{delete_admin['id']}", 204,
         token=admin, operation="/api/admins/{id}")
    call("alumno no crea admin", "POST", "/api/admins", 403, token=student, body={})

    # CRUD de carrera y asignatura.
    _, audit_career = call(
        "crear carrera", "POST", "/api/careers", 201, token=admin,
        body={"code": "AUD", "name": "Carrera Auditoría"}, operation="/api/careers",
    )
    career_id = audit_career["id"]
    call("obtener carrera", "GET", f"/api/careers/{career_id}", 200, token=admin,
         operation="/api/careers/{id}")
    call("actualizar carrera", "PUT", f"/api/careers/{career_id}", 200, token=admin,
         body={"code": "AUD", "name": "Carrera Auditoría Actualizada"},
         operation="/api/careers/{id}")
    call("asignaturas por carrera", "GET", f"/api/careers/{career_id}/subjects", 200,
         token=admin, operation="/api/careers/{id}/subjects")
    call("carrera inválida", "POST", "/api/careers", 400, token=admin,
         body={"code": "", "name": ""})
    _, delete_career = call("crear carrera eliminable", "POST", "/api/careers", 201,
                            token=admin, body={"code": "AUDDEL", "name": "Eliminar"})
    call("eliminar carrera", "DELETE", f"/api/careers/{delete_career['id']}", 204,
         token=admin, operation="/api/careers/{id}")
    call("alumno no crea carrera", "POST", "/api/careers", 403, token=student, body={})

    def create_subject(code: str, name: str) -> dict[str, Any]:
        _, item = call(
            f"crear asignatura {code}", "POST", "/api/subjects", 201, token=admin,
            body={"code": code, "name": name, "credits": 4, "careerId": career_id, "active": True},
            operation="/api/subjects" if code == "AUD101" else None,
        )
        return item

    audit_subject = create_subject("AUD101", "Auditoría 1")
    full_subject = create_subject("AUD102", "Auditoría 2")
    race_subject = create_subject("AUD103", "Auditoría Concurrente")
    call("obtener asignatura", "GET", f"/api/subjects/{audit_subject['id']}", 200,
         token=admin, operation="/api/subjects/{id}")
    call("actualizar asignatura", "PUT", f"/api/subjects/{audit_subject['id']}", 200,
         token=admin,
         body={"code": "AUD101", "name": "Auditoría 1A", "credits": 5, "careerId": career_id, "active": True},
         operation="/api/subjects/{id}")
    call("asignatura inválida", "POST", "/api/subjects", 400, token=admin,
         body={"code": "", "name": "", "credits": 0})
    delete_subject = create_subject("AUDDEL", "Eliminar")
    call("eliminar asignatura", "DELETE", f"/api/subjects/{delete_subject['id']}", 204,
         token=admin, operation="/api/subjects/{id}")
    call("alumno no crea asignatura", "POST", "/api/subjects", 403, token=student, body={})

    # Semestre planificado y validaciones temporales.
    semester_body = {
        "year": 2030, "period": "1S", "startDate": "2030-03-01", "endDate": "2030-07-31",
        "gradeStartDate": "2030-06-01", "gradeEndDate": "2030-07-31", "status": "PLANNED",
    }
    _, audit_semester = call("crear semestre", "POST", "/api/semesters", 201, token=admin,
                             body=semester_body, operation="/api/semesters")
    call("obtener semestre", "GET", f"/api/semesters/{audit_semester['id']}", 200,
         token=admin, operation="/api/semesters/{id}")
    semester_body["endDate"] = "2030-08-15"
    semester_body["gradeEndDate"] = "2030-08-15"
    call("actualizar semestre", "PUT", f"/api/semesters/{audit_semester['id']}", 200,
         token=admin, body=semester_body, operation="/api/semesters/{id}")
    call("semestre con fechas invertidas", "POST", "/api/semesters", 400, token=admin,
         body={"year": 2031, "period": "1S", "startDate": "2031-08-01", "endDate": "2031-03-01",
               "gradeStartDate": "2031-06-01", "gradeEndDate": "2031-07-01", "status": "PLANNED"})
    call("no cerrar semestre planificado", "POST", f"/api/semesters/{audit_semester['id']}/close", 400,
         token=admin)
    call("alumno no crea semestre", "POST", "/api/semesters", 403, token=student, body={})

    # CRUD geoespacial.
    polygon = '{"type":"Polygon","coordinates":[[[-70.70,-33.46],[-70.699,-33.46],[-70.699,-33.459],[-70.70,-33.459],[-70.70,-33.46]]]}'
    point = '{"type":"Point","coordinates":[-70.6995,-33.4595]}'
    call("crear edificio", "POST", "/api/buildings", 201, token=admin,
         body={"code": "AUDB", "name": "Edificio Auditoría", "geomGeoJson": polygon},
         operation="/api/buildings")
    _, buildings = call("relistar edificios", "GET", "/api/buildings", 200, token=admin)
    building = find(buildings, "code", "AUDB")
    call("obtener edificio", "GET", f"/api/buildings/{building['id']}", 200, token=admin,
         operation="/api/buildings/{id}")
    call("actualizar edificio", "PUT", f"/api/buildings/{building['id']}", 200, token=admin,
         body={"code": "AUDB", "name": "Edificio Auditoría A", "geomGeoJson": polygon},
         operation="/api/buildings/{id}")
    call("edificio GeoJSON inválido", "POST", "/api/buildings", {400, 409}, token=admin,
         body={"code": "BADG", "name": "Malo", "geomGeoJson": "no-es-geojson"})
    delete_polygon = '{"type":"Polygon","coordinates":[[[-70.71,-33.46],[-70.709,-33.46],[-70.709,-33.459],[-70.71,-33.459],[-70.71,-33.46]]]}'
    call("crear edificio eliminable", "POST", "/api/buildings", 201, token=admin,
         body={"code": "AUDBDEL", "name": "Eliminar", "geomGeoJson": delete_polygon})
    _, buildings = call("edificios para eliminar", "GET", "/api/buildings", 200, token=admin)
    delete_building = find(buildings, "code", "AUDBDEL")
    call("eliminar edificio", "DELETE", f"/api/buildings/{delete_building['id']}", 200,
         token=admin, operation="/api/buildings/{id}")

    call("crear sala", "POST", "/api/rooms", 201, token=admin,
         body={"buildingId": building["id"], "code": "AUD-R1", "name": "Sala Auditoría", "capacity": 12, "geomGeoJson": point},
         operation="/api/rooms")
    _, rooms = call("relistar salas", "GET", f"/api/rooms?buildingId={building['id']}", 200,
                    token=admin)
    room = find(rooms, "code", "AUD-R1")
    call("obtener sala", "GET", f"/api/rooms/{room['id']}", 200, token=admin,
         operation="/api/rooms/{id}")
    call("actualizar sala", "PUT", f"/api/rooms/{room['id']}", 200, token=admin,
         body={"buildingId": building["id"], "code": "AUD-R1", "name": "Sala Auditoría A", "capacity": 12, "geomGeoJson": point},
         operation="/api/rooms/{id}")
    call("sala con capacidad inválida", "POST", "/api/rooms", 400, token=admin,
         body={"buildingId": building["id"], "code": "BAD", "name": "Mala", "capacity": 0, "geomGeoJson": point})
    call("crear sala eliminable", "POST", "/api/rooms", 201, token=admin,
         body={"buildingId": building["id"], "code": "AUD-RDEL", "name": "Eliminar", "capacity": 5, "geomGeoJson": point})
    _, rooms = call("salas para eliminar", "GET", f"/api/rooms?buildingId={building['id']}", 200,
                    token=admin)
    delete_room = find(rooms, "code", "AUD-RDEL")
    call("eliminar sala", "DELETE", f"/api/rooms/{delete_room['id']}", 200, token=admin,
         operation="/api/rooms/{id}")

    call("crear POI", "POST", "/api/accessibility-pois", 201, token=admin,
         body={"name": "Rampa Auditoría", "buildingId": building["id"], "geomGeoJson": point},
         operation="/api/accessibility-pois")
    _, pois = call("listar POI", "GET", f"/api/accessibility-pois?buildingId={building['id']}", 200,
                   token=admin, operation="/api/accessibility-pois")
    poi = find(pois, "name", "Rampa Auditoría")
    call("obtener POI", "GET", f"/api/accessibility-pois/{poi['id']}", 200, token=admin,
         operation="/api/accessibility-pois/{id}")
    call("actualizar POI", "PUT", f"/api/accessibility-pois/{poi['id']}", 200, token=admin,
         body={"name": "Rampa Auditoría A", "buildingId": building["id"], "geomGeoJson": point},
         operation="/api/accessibility-pois/{id}")
    call("POI inválido", "POST", "/api/accessibility-pois", 400, token=admin, body={"name": ""})
    call("eliminar POI", "DELETE", f"/api/accessibility-pois/{poi['id']}", 200, token=admin,
         operation="/api/accessibility-pois/{id}")

    # Profesores y estudiantes de prueba.
    _, audit_professor = call(
        "crear profesor", "POST", "/api/professors", 200, token=admin,
        body={"name": "Profe Auditor", "email": "audit.prof@usach.cl", "password": "Audit123!", "department": "Auditoría", "rut": "70000001-1"},
        operation="/api/professors",
    )
    professor_id = audit_professor["id"]
    audit_prof_token = login("audit.prof@usach.cl", "Audit123!")
    call("obtener profesor", "GET", f"/api/professors/{professor_id}", 200, token=admin,
         operation="/api/professors/{id}")
    call("actualizar profesor", "PUT", f"/api/professors/{professor_id}", 200, token=admin,
         body={"name": "Profe Auditor A", "email": "audit.prof@usach.cl", "password": "", "department": "Auditoría A", "rut": "70000001-1"},
         operation="/api/professors/{id}")
    _, delete_prof = call("crear profesor eliminable", "POST", "/api/professors", 200,
                          token=admin, body={"name": "Profe Borrar", "email": "audit.prof.delete@usach.cl", "password": "Audit123!", "department": "Auditoría", "rut": "70000001-2"})
    call("eliminar profesor", "DELETE", f"/api/professors/{delete_prof['id']}", 204,
         token=admin, operation="/api/professors/{id}")
    call("profesor inválido", "POST", "/api/professors", 400, token=admin,
         body={"name": "", "email": "", "password": "", "department": ""})

    student_body = {
        "rut": "70000002-1", "email": "audit.student@usach.cl", "password": "Audit123!",
        "firstName": "Alumno", "lastName": "Auditor", "enrollmentNumber": "AUD-001",
    }
    call("crear estudiante", "POST", "/api/students", 201, token=admin, body=student_body,
         operation="/api/students")
    _, students = call("relistar estudiantes", "GET", "/api/students", 200, token=admin)
    audit_student = find(students, "enrollmentNumber", "AUD-001")
    student_id = audit_student["id"]
    audit_student_token = login("audit.student@usach.cl", "Audit123!")
    call("obtener estudiante", "GET", f"/api/students/{student_id}", 200, token=admin,
         operation="/api/students/{id}")
    call("actualizar estudiante", "PUT", f"/api/students/{student_id}", 200, token=admin,
         body={"usuarioId": audit_student["usuarioId"], "enrollmentNumber": "AUD-001", "firstName": "Alumno", "lastName": "Auditor A", "academicStatus": "ACTIVE"},
         operation="/api/students/{id}")
    call("malla estudiante", "GET", f"/api/students/{student_id}/curriculum", 200,
         token=audit_student_token, operation="/api/students/{id}/curriculum")
    call("actualizar ubicación", "PATCH", f"/api/students/{student_id}/location", 200,
         token=audit_student_token, body={"latitude": -33.4595, "longitude": -70.6995},
         operation="/api/students/{id}/location")
    call("obtener ubicación", "GET", f"/api/students/{student_id}/location", 200,
         token=audit_student_token, operation="/api/students/{id}/location")
    call("ubicación fuera de rango", "PATCH", f"/api/students/{student_id}/location", 400,
         token=audit_student_token, body={"latitude": 91, "longitude": 0})
    call("leer notas ajenas", "GET", "/api/grades/student/2", 403, token=student)

    delete_student_body = {
        "rut": "70000002-2", "email": "audit.student.delete@usach.cl", "password": "Audit123!",
        "firstName": "Alumno", "lastName": "Borrar", "enrollmentNumber": "AUD-DEL",
    }
    call("crear estudiante eliminable", "POST", "/api/students", 201, token=admin,
         body=delete_student_body)
    _, students = call("estudiantes para eliminar", "GET", "/api/students", 200, token=admin)
    delete_student = find(students, "enrollmentNumber", "AUD-DEL")
    call("eliminar estudiante", "DELETE", f"/api/students/{delete_student['id']}", 200,
         token=admin, operation="/api/students/{id}")
    call("estudiante inválido", "POST", "/api/students", 400, token=admin,
         body={"rut": "", "email": "", "password": ""})

    # Secciones, horarios y conflictos.
    section_body = {
        "subjectId": audit_subject["id"], "professorId": professor_id,
        "semesterId": current_semester["id"], "totalSeats": 10, "roomId": room["id"],
        "dayOfWeek": 6, "startTime": "21:25:00", "endTime": "22:45:00",
    }
    _, audit_section = call("crear sección", "POST", "/api/sections", 201, token=admin,
                            body=section_body, operation="/api/sections")
    section_id = audit_section["id"]
    call("obtener sección", "GET", f"/api/sections/{section_id}", 200, token=admin,
         operation="/api/sections/{id}")
    section_body["startTime"] = "20:05:00"
    section_body["endTime"] = "21:25:00"
    call("actualizar sección", "PUT", f"/api/sections/{section_id}", 200, token=admin,
         body=section_body, operation="/api/sections/{id}")
    call("sección fuera de bloque", "POST", "/api/sections", 400, token=admin,
         body={**section_body, "startTime": "10:00:00", "endTime": "11:00:00"})
    call("sección excede capacidad", "POST", "/api/sections", 400, token=admin,
         body={**section_body, "dayOfWeek": 5, "totalSeats": 999})
    call("conflicto de sala/profesor", "POST", "/api/sections", 400, token=admin,
         body=section_body)
    call("secciones profesor", "GET", f"/api/sections/professor/{professor_id}", 200,
         token=audit_prof_token, operation="/api/sections/professor/{professorId}")
    call("secciones activas profesor", "GET", f"/api/sections/professor/{professor_id}/active", 200,
         token=audit_prof_token, operation="/api/sections/professor/{professorId}/active")
    call("secciones profesor vía recurso", "GET", f"/api/professors/{professor_id}/sections", 200,
         token=audit_prof_token, operation="/api/professors/{id}/sections")
    call("profesor no ve secciones ajenas", "GET", "/api/sections/professor/2/active", 403,
         token=professor)

    delete_section_body = {**section_body, "startTime": "18:45:00", "endTime": "20:05:00"}
    _, delete_section = call("crear sección eliminable", "POST", "/api/sections", 201,
                             token=admin, body=delete_section_body)
    call("eliminar sección", "DELETE", f"/api/sections/{delete_section['id']}", 200,
         token=admin, operation="/api/sections/{id}")

    # Inscripción directa y por procedimiento, historial, cupos y permisos.
    _, enrollment_message = call(
        "crear inscripción por ruta base", "POST", "/api/enrollments", 201,
        token=audit_student_token, body={"studentId": student_id, "sectionId": section_id},
        operation="/api/enrollments",
    )
    require("enrolled" in str(enrollment_message).lower(), "ruta base usa inscripción segura",
            enrollment_message)
    _, enrollments = call("inscripciones estudiante", "GET", f"/api/enrollments/student/{student_id}", 200,
                          token=audit_student_token, operation="/api/enrollments/student/{studentId}")
    enrollment = next(item for item in enrollments if item["sectionId"] == section_id)
    enrollment_id = enrollment["id"]
    call("obtener inscripción", "GET", f"/api/enrollments/{enrollment_id}", 200,
         token=audit_student_token, operation="/api/enrollments/{id}")
    call("inscritos de sección", "GET", f"/api/enrollments/section/{section_id}", 200,
         token=audit_prof_token, operation="/api/enrollments/section/{sectionId}")
    call("secciones de estudiante", "GET", f"/api/sections/student/{student_id}", 200,
         token=audit_student_token, operation="/api/sections/student/{studentId}")
    call("inscripción duplicada", "POST", "/api/enrollments/enroll", 400,
         token=audit_student_token, body={"studentId": student_id, "sectionId": section_id})
    call("alumno no inscribe a otro", "POST", "/api/enrollments/enroll", 403,
         token=audit_student_token, body={"studentId": 1, "sectionId": section_id})
    call("inscripción inexistente", "POST", "/api/enrollments/enroll", 400,
         token=admin, body={"studentId": 999999, "sectionId": 999999})
    call("estado inválido", "PATCH", f"/api/enrollments/{enrollment_id}/status", 400,
         token=admin, body="INVALID")
    call("cancelar inscripción", "DELETE", f"/api/enrollments/{enrollment_id}", 200,
         token=audit_student_token, operation="/api/enrollments/{id}")
    call("cancelar dos veces", "DELETE", f"/api/enrollments/{enrollment_id}", 409,
         token=audit_student_token)
    call("reinscribir cancelada", "POST", "/api/enrollments/enroll", 201,
         token=audit_student_token, body={"studentId": student_id, "sectionId": section_id},
         operation="/api/enrollments/enroll")
    call("secciones cercanas", "GET",
         f"/api/enrollments/nearby-sections?subjectId={audit_subject['id']}&lat=-33.4595&lng=-70.6995",
         200, token=audit_student_token, operation="/api/enrollments/nearby-sections")

    # Notas: profesor responsable, rango, duplicado, actualización y lectura propia.
    _, grade = call(
        "profesor registra nota", "POST", "/api/professors/grade?professorRut=70000001-1", 200,
        token=audit_prof_token, body={"enrollmentId": enrollment_id, "value": 5.5},
        operation="/api/professors/grade",
    )
    grade_id = grade["id"]
    call("actualizar nota", "PUT", f"/api/grades/{grade_id}", 200, token=audit_prof_token,
         body={"enrollmentId": enrollment_id, "value": 5.8}, operation="/api/grades/{id}")
    call("nota fuera de rango", "POST", "/api/grades", 400, token=admin,
         body={"enrollmentId": enrollment_id, "value": 8.0})
    call("nota duplicada", "POST", "/api/grades", 409, token=admin,
         body={"enrollmentId": enrollment_id, "value": 6.0}, operation="/api/grades")
    call("profesor no califica sección ajena", "POST", "/api/professors/grade?professorRut=11222333-4",
         403, token=professor, body={"enrollmentId": 35, "value": 5.0})
    call("completar inscripción", "PATCH", f"/api/enrollments/{enrollment_id}/status", 200,
         token=admin, body="COMPLETED", operation="/api/enrollments/{id}/status")
    call("notas propias", "GET", f"/api/grades/student/{student_id}", 200,
         token=audit_student_token, operation="/api/grades/student/{studentId}")

    # Sección llena: el segundo alumno debe fallar y no aparecer como cercana.
    full_section_body = {
        **section_body,
        "subjectId": full_subject["id"], "dayOfWeek": 6,
        "startTime": "18:45:00", "endTime": "20:05:00", "totalSeats": 1,
    }
    _, full_section = call("crear sección de un cupo", "POST", "/api/sections", 201,
                           token=admin, body=full_section_body)
    call("ocupar último cupo", "POST", "/api/enrollments/enroll", 201,
         token=audit_student_token, body={"studentId": student_id, "sectionId": full_section["id"]})
    call("rechazar sección llena", "POST", "/api/enrollments/enroll", 400,
         token=admin, body={"studentId": 1, "sectionId": full_section["id"]})
    _, nearby_full = call("no ofrecer sección llena", "GET",
                          f"/api/enrollments/nearby-sections?subjectId={full_subject['id']}&lat=-33.4595&lng=-70.6995",
                          200, token=audit_student_token)
    require(not any(item["sectionId"] == full_section["id"] for item in nearby_full),
            "sección llena excluida de cercanas", nearby_full)
    _, full_enrollments = call("obtener inscripción llena", "GET",
                               f"/api/enrollments/student/{student_id}", 200,
                               token=audit_student_token)
    full_enrollment = next(item for item in full_enrollments if item["sectionId"] == full_section["id"])
    call("cancelar sección llena", "DELETE", f"/api/enrollments/{full_enrollment['id']}", 200,
         token=audit_student_token)

    # Carrera por el último cupo: ambas solicitudes parten en paralelo y el
    # procedimiento con SELECT ... FOR UPDATE debe aceptar exactamente una.
    race_section_body = {
        **section_body,
        "subjectId": race_subject["id"], "dayOfWeek": 6,
        "startTime": "16:55:00", "endTime": "18:15:00", "totalSeats": 1,
    }
    _, race_section = call("crear sección para concurrencia", "POST", "/api/sections", 201,
                           token=admin, body=race_section_body)

    def concurrent_enroll(student_id_value: int) -> tuple[int, Any]:
        return call(
            f"inscripción concurrente estudiante {student_id_value}",
            "POST", "/api/enrollments/enroll", {201, 400}, token=admin,
            body={"studentId": student_id_value, "sectionId": race_section["id"]},
        )

    with ThreadPoolExecutor(max_workers=2) as executor:
        concurrent_results = list(executor.map(concurrent_enroll, (1, student_id)))
    concurrent_statuses = sorted(status for status, _ in concurrent_results)
    require(concurrent_statuses == [201, 400], "un solo ganador por el último cupo",
            concurrent_results)
    _, race_state = call("consultar sección tras carrera", "GET",
                         f"/api/sections/{race_section['id']}", 200, token=admin)
    require(race_state["availableSeats"] == 0, "cupo nunca negativo tras carrera", race_state)
    _, race_enrollments = call("inscritos tras carrera", "GET",
                               f"/api/enrollments/section/{race_section['id']}", 200,
                               token=admin)
    require(len([item for item in race_enrollments if item["status"] == "ACTIVE"]) == 1,
            "una sola inscripción activa tras carrera", race_enrollments)
    race_winner = next(item for item in race_enrollments if item["status"] == "ACTIVE")
    call("cancelar ganador de carrera", "DELETE", f"/api/enrollments/{race_winner['id']}", 200,
         token=admin)

    # Ubicación sin fixture temporal: 404 es correcto fuera del horario de clase.
    call("sala más cercana", "POST", "/api/location/nearest-room", {200, 404},
         token=audit_student_token,
         body={"studentId": student_id, "lat": -33.4595, "lng": -70.6995},
         operation="/api/location/nearest-room")
    call("coordenada inválida sala cercana", "POST", "/api/location/nearest-room", 400,
         token=audit_student_token, body={"studentId": student_id, "lat": 100, "lng": 0})
    call("alumno consulta ubicación de otro", "POST", "/api/location/nearest-room", 403,
         token=audit_student_token, body={"studentId": 1, "lat": -33.45, "lng": -70.68})

    # Completar notas vigentes y cerrar el semestre al final de la auditoría.
    for enrollment_id_seed, value in ((34, 5.0), (35, 5.1), (36, 5.2), (37, 5.3)):
        call(f"nota de cierre {enrollment_id_seed}", "POST", "/api/grades", 200,
             token=admin, body={"enrollmentId": enrollment_id_seed, "value": value})
    call("cerrar semestre vigente", "POST", f"/api/semesters/{current_semester['id']}/close", 200,
         token=admin, operation="/api/semesters/{id}/close")
    call("cerrar semestre dos veces", "POST", f"/api/semesters/{current_semester['id']}/close", 400,
         token=admin)

    # Verificar cobertura contra el contrato OpenAPI actual.
    _, spec = call("OpenAPI final", "GET", "/v3/api-docs", 200)
    operations = {
        (method, path)
        for path, methods in spec["paths"].items()
        for method in methods
        if method in {"get", "post", "put", "patch", "delete"}
    }
    missing = sorted(operations - covered)
    require(not missing, "cobertura de las 81 operaciones OpenAPI", missing)

    failed = [result for result in results if not result.ok]
    print(f"API audit: {len(results) - len(failed)}/{len(results)} checks OK")
    print(f"OpenAPI operations covered: {len(covered & operations)}/{len(operations)}")
    if failed:
        print("\nFAILED CHECKS:")
        for result in failed:
            print(
                f"- {result.name}: HTTP {result.status}, expected {sorted(result.expected)}, "
                f"body={result.body!r}"
            )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
