# Sistema de Administración Académica Universitaria

**Laboratorio 3 — MongoDB: Bases de Datos NoSQL Avanzadas**

Taller de Base de Datos (TBD) Diurno · 1-2026 · **Grupo 2**

Sistema web de gestión académica (estudiantes, profesores, asignaturas, secciones, inscripciones y notas) migrado de MySQL a **MongoDB 7.0** sobre un **Replica Set**, con validación de esquema (`$jsonSchema`), transacciones multi-documento ACID, índices diseñados, pipeline de agregación (`$group` + `$bucket`) y una vista materializada mantenida con **Change Streams**.

---

## 1. Arquitectura y tecnologías

```
┌──────────────┐       HTTP/JWT        ┌─────────────────────┐       MongoDB wire protocol       ┌──────────────────────────────┐
│  Frontend    │ ────────────────────► │      Backend        │ ───────────────────────────────► │  MongoDB 7.0 (Replica Set rs0) │
│  React 18    │    localhost:3000     │  Java 21 / Spring 4 │   mongodb://primary,secondary/   │  mongo-primary :27017 (P)     │
│  (SPA + JWT) │                       │  localhost:9090     │      ?replicaSet=rs0            │  mongo-secondary:27017 (S)    │
└──────────────┘                       └─────────────────────┘                                  │  BBDD: academic_mongo          │
                                                                                                 └──────────────────────────────┘
```

| Capa | Tecnología | Detalle |
| --- | --- | --- |
| Frontend | React 18 (CRA), React Router 6, Axios | JWT en `localStorage`, navbar por rol |
| Backend | Java 21 · Spring Boot 4 | Driver **MongoDB Java oficial** (no Spring Data) |
| BD | MongoDB 7.0 | Replica Set `rs0` (1 primario + 1 secundario) |
| Despliegue | Docker Compose | 8 servicios con orden de arranque por dependencias |

**Replica Set** (requisito del enunciado): habilita transacciones multi-documento y Change Streams. La inicialización la hace el servicio `mongo-init` con `database/mongo/0_init_replica_set.js`.

---

## 2. Manual de instalación (verificado)

### 2.1 Requisitos

- Docker Engine + Docker Compose (WSL2 recomendado en Windows).
- Puerto libre: `3000`, `9090`, `27017`, `27018`.

### 2.2 Configuración (`.env`)

Copiar `.env.example` a `.env`. Variables usadas:

| Variable | Valor por defecto | Descripción |
| --- | --- | --- |
| `BACKEND_HOST_PORT` | `9090` | Puerto expuesto del backend |
| `BACKEND_CONTAINER_PORT` | `9090` | Puerto interno del backend |
| `FRONTEND_HOST_PORT` | `3000` | Puerto del frontend |
| `MONGO_PRIMARY_HOST_PORT` | `27017` | Puerto host del primario |
| `MONGO_SECONDARY_HOST_PORT` | `27018` | Puerto host del secundario |
| `MONGO_REPLICA_SET` | `rs0` | Nombre del replica set |
| `MONGO_DATABASE` | `academic_mongo` | Base de datos |
| `REACT_APP_API_URL` | `http://localhost:9090` | URL de la API (frontend) |

### 2.3 Despliegue

```bash
docker compose up -d --build
```

Orden automático de arranque:

1. `mongo-primary` y `mongo-secondary` (healthcheck con `ping`).
2. `mongo-init` → ejecuta `0_init_replica_set.js` (configura `rs0`).
3. `mongo-schema` → `1_collections_and_schema.js` (colecciones + validadores `$jsonSchema`).
4. `mongo-seed-users` → `1_5_seed_users.js` (usuarios con contraseñas hasheadas BCrypt).
5. `mongo-indexes` → `2_indexes.js` (índices de la sección 5).
6. `mongo-seed` → `3_seed_mongo.js` (dominio académico de prueba).
7. `backend` (:9090) → arranca el driver, reconstruye la vista materializada y abre el Change Stream.
8. `frontend` (:3000) → sirve la SPA.

### 2.4 Verificación

```bash
docker compose ps
# Esperar: mongo-primary (healthy), mongo-secondary (healthy), backend, frontend

# Estado del replica set
docker exec -it mongo_primary mongosh --eval "rs.status()"

# Colecciones creadas
docker exec -it mongo_primary mongosh "mongodb://localhost:27017/academic_mongo?replicaSet=rs0" --eval "show collections"
```

- Backend: `http://localhost:9090/api/auth/login`
- Frontend: `http://localhost:3000`

### 2.5 Credenciales de prueba

Todos los usuarios usan la contraseña `1234` (hash BCrypt sembrado en `1_5_seed_users.js`).

| Usuario | RUT | Rol |
| --- | --- | --- |
| `admin@usach.cl` | `11111111-1` | ADMIN |
| `juan@usach.cl` | `12345678-9` | STUDENT (id 1001) |
| `carlos@usach.cl` | `11222333-4` | PROFESSOR (id 3001) |

---

## 3. Modelado de datos (embedding vs referencing)

| Colección | Documento | Relación |
| --- | --- | --- |
| `users` | Autenticación (email, passwordHash, rol) | — |
| `students` | Datos académicos del estudiante | 1:1 con `users` |
| `professors` | Datos académicos del profesor | 1:1 con `users` |
| `subjects` | Catálogo de asignaturas | — |
| `semesters` | Periodos académicos | — |
| `sections` | Oferta de secciones por semestre | Referencia a `subjects` y `semesters` |
| `enrollments` | Inscripciones con estado | Referencia a `students`, `sections` |
| `grades` | Calificaciones finales | Referencia a `enrollments`, `students`, `subjects`, `semesters` |
| `certificados_notas` | **Vista materializada** de certificados | Agregado por estudiante |

### Decisión: notas **referenciadas** (no embebidas)

Las calificaciones viven en una colección independiente (`grades`) y se referencian por `enrollmentId`, `studentId`, `subjectId` y `semesterId`. Justificación:

- **Alto volumen de escritura** al registrar notas; embederlas inflaría el documento del estudiante y reescribiría todo el historial en cada alta.
- Las notas se consultan **agregadas por asignatura/semestre** (reportes y certificados), no junto al estudiante → colección dedicada + pipeline.
- Se mantiene consistencia con el modelo transaccional de inscripciones.

---

## 4. Validación de esquema (`$jsonSchema`)

Creada en `database/mongo/1_collections_and_schema.js` con `validationLevel: "strict"` y `validationAction: "error"` para **todas** las colecciones. Ejemplos de restricciones:

- `grades.nota`: número entre `1.0` y `7.0`, campos requeridos y `additionalProperties: false` (el documento no acepta campos fuera del contrato).
- `enrollments`: `studentId` y `sectionId` requeridos, `status` en `[ACTIVE, CANCELLED]`.
- `sections`: `cupos` entero > 0.
- Referencias `studentId`, `subjectId`, etc. tipadas como ObjectId.

> **Hallazgo de validación:** el insert de nota original incluía `sectionId`, campo no declarado en el `$jsonSchema` de `grades` → el insert fallaba con error 121. Se corrigió el backend (`MongoGradeRepository`) para alinearlo con el contrato.

---

## 5. Estrategia de índices

Creada en `database/mongo/2_indexes.js`. Cada índice tiene un propósito medido con `explain()`:

| Colección | Índice | Tipo | Propósito |
| --- | --- | --- | --- |
| `users` | `uniq_user_id` | único | Acceso por id de usuario |
| `users` | `uniq_user_email` | único | Login por email (JWT) |
| `enrollments` | `uniq_enrollment_student_section` | **único compuesto** | Impide doble inscripción (a nivel BD, respaldo del chequeo transaccional) |
| `enrollments` | `sortedByCourse_and_academicPeriod` | compuesto | Horario del estudiante ordenado por asignatura y periodo |
| `grades` | `uniq_grade_enrollment` | único | Una sola nota por inscripción |
| `subjects` | `text_subject_name` | texto | Buscador del catálogo (`$text`, consultas en sección 6.5) |

---

## 6. Funcionalidades MongoDB implementadas

### 6.1 Transacción multi-documento ACID

`EnrollmentTransactionService` (`ClientSession` + `withTransaction`). Al inscribir se ejecutan, dentro de la misma transacción:

1. Validar que los **prerrequisitos** estén aprobados (consulta `grades`).
2. Validar **cupo** disponible en la sección y **decrementarlo**.
3. Insertar la inscripción.
4. Si algo falla → **rollback** total (sin cupos ni inscripciones a medias).

Validado E2E: inscripción duplicada → 400; prerrequisito faltante → 400; cancelación → 200 y restaura el cupo.

### 6.2 Aggregation Pipeline ($group + $bucket)

`GET /api/professors/reports` — reporte de reprobación por **asignatura y semestre** en `MongoGradeRepository.getFailureRateReport()`:

```
$match  (notas numéricas)
  → $bucket (rangos [0,4) reprobado | [4,7] aprobado, default "aprobado")
  → $unwind
  → $group por { subjectId, semesterId }
  → $lookup (subjects, semesters)
  → $project (% de reprobación)
  → $sort (año, periodo)
```

Filtros opcionales `?semesterId=` y `?subjectId=`.

### 6.3 Change Streams + Vista materializada

- `CertificateChangeStreamService` se suscribe a cambios en `grades`.
- Ante cada alta/modificación de nota, **recomputa el certificado del estudiante** y hace `$merge` sobre `certificados_notas`.
- `GET /api/certificates/{studentId}` expone la vista (estudiante → solo la propia; admin → cualquiera).
- Script `database/mongo/4_certificates_merge.js` reconstruye la vista completa (drop + `$merge`):

```bash
docker cp database/mongo/4_certificates_merge.js mongo_primary:/tmp/4_certificates_merge.js
docker exec -it mongo_primary mongosh "mongodb://localhost:27017/academic_mongo?replicaSet=rs0" /tmp/4_certificates_merge.js
```

Verificado E2E: al registrar una nota nueva el certificado pasa de 6 a 7 ramos de forma reactiva (~1 s).

### 6.4 Búsqueda de texto ($text)

`GET /api/subjects/search?q=...` sobre el índice `text_subject_name`. Los resultados se ordenan por **relevancia** (`$meta: "textScore"`).

---

## 7. Documentación de la API

Base URL: `http://localhost:9090`. Autenticación por **JWT Bearer** (`POST /api/auth/login`). Ejemplo:

```bash
curl -s -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@usach.cl","password":"1234"}'
```

| Método | Endpoint | Rol | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | público | Inicio de sesión → JWT |
| `GET` | `/api/subjects/search?q=` | autenticado | Búsqueda `$text` en catálogo |
| `POST` | `/api/enrollments/enroll` | estudiante | Inscripción transaccional (ACID) |
| `DELETE` | `/api/enrollments/{id}` | estudiante | Cancelación (restaura cupo) |
| `GET` | `/api/enrollments/student/{studentId}` | propio | Inscripciones del estudiante |
| `GET` | `/api/students/{studentId}/curriculum` | propio | Avance curricular |
| `POST` | `/api/professors/grade` | profesor | Registrar nota (dispara Change Stream) |
| `GET` | `/api/grades/student/{studentId}` | profesor | Notas de un estudiante |
| `GET` | `/api/professors/reports` | admin/profesor | **Reprobación por asignatura×semestre** ($group + $bucket) |
| `GET` | `/api/professors/reports?semesterId=` | admin/profesor | Filtro por semestre |
| `GET` | `/api/professors/reports?subjectId=` | admin/profesor | Filtro por asignatura |
| `GET` | `/api/certificates/{studentId}` | propio/admin | Certificado de notas materializado |

### Ejemplos de respuesta

`GET /api/professors/reports`:

```jsonc
[
  {
    "subjectId": "6a78c74054010de10c835efa",
    "subjectCode": "ALG1",
    "subjectName": "Álgebra 1",
    "semesterId": "6a78c74054010de10c835efe",
    "semesterYear": 2024,
    "semesterPeriod": "1S",
    "totalGrades": 4,
    "failedGrades": 2,
    "failurePercentage": 50.0
  }
]
```

`GET /api/certificates/1001`:

```jsonc
{
  "_id": "6a78cf9a58c69118e25d9e47",
  "entries": [
    {
      "subjectCode": "ALG1",
      "subjectName": "Álgebra 1",
      "semesterYear": 2024,
      "semesterPeriod": "1S",
      "grade": 5.2,
      "recordedAt": "2024-07-01T00:00:00.000Z"
    }
  ],
  "totalRamos": 6,
  "promedioGeneral": 5.2,
  "updatedAt": "2026-08-09T19:06:07.837Z"
}
```

`GET /api/subjects/search?q=algebra`:

```jsonc
[
  { "code": "ALG2", "name": "Álgebra 2" },
  { "code": "ALG1", "name": "Álgebra 1" }
]
```

---

## 8. Funcionalidades por rol (frontend)

| Rol | Funcionalidades |
| --- | --- |
| **Estudiante** | Login, inscribirse en secciones (transacción), ver su horario, avance curricular, notas y **certificado de notas materializado** |
| **Profesor** | Login, ver sus secciones e inscritos, registrar notas, **reporte de reprobación** por asignatura×semestre con filtros |
| **Admin** | Todo lo anterior + **búsqueda `$text`** del catálogo de asignaturas |

---

## 9. Estructura del repositorio

```
academic-management-system
|-- docker-compose.yml
|-- .env / .env.example
|-- README.md
|-- database/mongo/         # Scripts MongoDB (replica set, schema, índices, seeds, merge)
|   |-- 0_init_replica_set.js
|   |-- 1_collections_and_schema.js
|   |-- 1_5_seed_users.js
|   |-- 2_indexes.js
|   |-- 3_seed_mongo.js
|   |-- 4_certificates_merge.js
|-- backend/                # API REST Java 21 / Spring Boot 4 (driver MongoDB oficial)
|   |-- src/main/java/usach/cl/demo/
|   |   |-- config/         # MongoConfig (replica set + write concern), SecurityConfig, JWT
|   |   |-- controller/     # REST controllers (enrollments, professors, certificates, ...)
|   |   |-- service/        # Services + EnrollmentTransactionService + CertificateChangeStreamService
|   |   |-- repository/     # Repos MongoDB (agregaciones $group/$bucket, $text, $merge)
|   |   |-- dto/            # FailureRateDTO, etc.
|   |   |-- model/          # Entidades + documentos MongoDB
|   |-- src/main/resources/application.properties
|-- frontend/               # React 18
|   |-- src/router/AppRouter.jsx
|   |-- src/pages/reports/FailureReport.jsx
|   |-- src/pages/students/StudentCertificate.jsx
|   |-- src/pages/subjects/SubjectList.jsx
|   |-- src/components/Navbar.jsx
|-- Presentacion/           # Slides de la defensa (Laboratorio3_MongoDB.pdf)
```

---

## 10. Tareas del enunciado → implementación

| # | Tarea | Implementación |
| - | --- | --- |
| 1 | Modelado de datos (embedding vs referencing) | Notas **referenciadas** en colección `grades` (sección 3) |
| 2 | Validación de esquema (`$jsonSchema`) | `1_collections_and_schema.js`, validador en todas las colecciones |
| 3 | Transacción multi-documento ACID | `EnrollmentTransactionService` (sección 6.1) |
| 4 | Aggregation Pipeline (`$group` + `$bucket`) | `getFailureRateReport()` (sección 6.2) |
| 5 | Índices compuestos/únicos + texto | `2_indexes.js` (sección 5) |
| 6 | Vista materializada + Change Streams | `CertificateChangeStreamService` + `certificados_notas` (sección 6.3) |

---

## 11. Integrantes

| Integrante | Rol |
| --- | --- |
| [Nombre 1] | Backend API REST + MongoDB |
| [Nombre 2] | Frontend React |
| [Nombre 3] | Scripts BD Mongo / Validación de esquema |
| [Nombre 4] | Documentación / Presentación |
| [Nombre 5] | Pruebas E2E |
