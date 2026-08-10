# Sistema de Administración Académica Universitaria

**Laboratorio 3 — MongoDB: Bases de Datos NoSQL Avanzadas**

Taller de Base de Datos (TBD) Diurno · 1-2026 · **Grupo 2**

Sistema web de gestión académica para estudiantes, profesores y administradores. Permite administrar carreras, asignaturas, semestres, secciones, inscripciones y notas; además incorpora reportes, búsqueda de texto y certificados académicos.

Para esta entrega se adoptó una arquitectura **Full MongoDB**: MongoDB es la única base de datos usada durante la ejecución. El sistema funciona sobre un Replica Set e implementa validación de esquema, transacciones ACID, índices, pipelines de agregación y una vista materializada actualizada con Change Streams.

---

## 1. Arquitectura y tecnologías

```text
┌──────────────────┐       HTTP + JWT       ┌─────────────────────────┐
│ Frontend         │ ─────────────────────► │ Backend                 │
│ React 19         │    localhost:3000      │ Java 21 / Spring Boot 4 │
└──────────────────┘                        │ localhost:9090          │
                                            └────────────┬────────────┘
                                                         │
                                                         │ Driver oficial de MongoDB
                                                         ▼
                              ┌─────────────────────────────────────────┐
                              │ MongoDB 7.0 · Replica Set rs0          │
                              │ Primary   localhost:27017              │
                              │ Secondary localhost:27018              │
                              │ Base de datos: academic_mongo          │
                              └─────────────────────────────────────────┘
```

| Capa | Tecnología | Uso en el proyecto |
| --- | --- | --- |
| Frontend | React 19, React Router 7, Axios | Interfaz web por rol y consumo de la API |
| Backend | Java 21, Spring Boot 4 | API REST, reglas académicas, seguridad y transacciones |
| Acceso a datos | Driver oficial de MongoDB para Java | Operaciones, consultas y agregaciones sin ORM/ODM |
| Base de datos | MongoDB 7.0 | Persistencia completa del sistema académico |
| Seguridad | JWT y BCrypt | Autenticación sin sesión y contraseñas cifradas |
| Despliegue | Docker Compose | Levanta y conecta automáticamente los 9 servicios |

El **Replica Set `rs0`** está compuesto por un nodo primario y uno secundario. Es necesario para ejecutar transacciones entre varios documentos y escuchar cambios mediante Change Streams. El servicio `mongo-init` realiza su configuración automáticamente.

> Aunque el repositorio conserva algunos archivos históricos de laboratorios anteriores, PostgreSQL y PostGIS no forman parte del despliegue ni son utilizados por la aplicación de Lab 3.

---

## 2. Manual de instalación y ejecución

### 2.1 Requisitos

- Docker Engine.
- Docker Compose V2, disponible mediante el comando `docker compose`.
- Puertos locales libres: `3000`, `9090`, `27017` y `27018`.

No es necesario instalar Java, Maven, Node.js ni MongoDB directamente en el equipo si se utiliza Docker.

### 2.2 Configuración

El repositorio incluye un archivo `.env` preparado para la ejecución local. Antes de levantar el sistema, verificar estos valores:

| Variable | Valor local | Descripción |
| --- | --- | --- |
| `BACKEND_HOST_PORT` | `9090` | Puerto público del backend |
| `BACKEND_CONTAINER_PORT` | `9090` | Puerto interno del backend |
| `FRONTEND_HOST_PORT` | `3000` | Puerto público del frontend |
| `MONGO_PRIMARY_HOST_PORT` | `27017` | Puerto del nodo primario |
| `MONGO_SECONDARY_HOST_PORT` | `27018` | Puerto del nodo secundario |
| `MONGO_REPLICA_SET` | `rs0` | Nombre del Replica Set |
| `MONGO_DATABASE` | `academic_mongo` | Nombre de la base de datos |
| `REACT_APP_API_URL` | `http://localhost:9090` | Dirección de la API usada por React |

### 2.3 Primer despliegue limpio

Desde la raíz del proyecto:

```bash
docker compose down -v --remove-orphans
docker compose up -d --build
```

El primer comando elimina contenedores y volúmenes anteriores para que los esquemas, índices y datos de prueba se creen desde cero. Por esa razón, también elimina datos locales existentes.

El arranque ocurre en este orden:

1. `mongo-primary` y `mongo-secondary` inician y esperan sus controles de salud.
2. `mongo-init` configura el Replica Set `rs0`.
3. `mongo-schema` crea las colecciones y validaciones `$jsonSchema`.
4. `mongo-seed-users` crea usuarios con contraseñas BCrypt.
5. `mongo-indexes` crea índices únicos, compuestos, de texto y TTL.
6. `mongo-seed` carga el escenario académico de prueba.
7. `backend` inicia la API, reconstruye los certificados y abre el Change Stream.
8. `frontend` publica la aplicación web.

Aunque se enumeran ocho etapas, Docker Compose administra **nueve servicios**, porque los dos nodos de MongoDB son servicios separados.

### 2.4 Verificación del despliegue

```bash
# Revisar el estado de todos los contenedores
docker compose ps

# Comprobar que el backend responde
curl -o /dev/null -w "HTTP %{http_code}\n" http://localhost:9090/v3/api-docs

# Ver los miembros del Replica Set
docker exec mongo_primary mongosh --quiet --eval \
  'rs.status().members.map(m => ({name: m.name, state: m.stateStr}))'

# Listar las colecciones creadas
docker exec mongo_primary mongosh --quiet --eval \
  'db.getSiblingDB("academic_mongo").getCollectionNames().sort()'
```

El segundo comando debe responder `HTTP 200`. Luego se puede ingresar a:

- Aplicación web: `http://localhost:3000`
- Swagger UI: `http://localhost:9090/swagger-ui/index.html`
- Especificación OpenAPI: `http://localhost:9090/v3/api-docs`

Para revisar errores de inicio:

```bash
docker compose logs -f backend
```

Para detener el sistema sin borrar sus volúmenes:

```bash
docker compose down
```

### 2.5 Credenciales de prueba

Todos los usuarios del mock usan la contraseña `1234`.

| Correo | RUT | Rol | Identificador |
| --- | --- | --- | --- |
| `admin@usach.cl` | `11111111-1` | ADMIN | `2001` |
| `juan@usach.cl` | `12345678-9` | STUDENT | `1001` |
| `carlos@usach.cl` | `11222333-4` | PROFESSOR | `3001` |

El mock completo contiene 12 usuarios: 6 estudiantes, 4 profesores y 2 administradores. También carga 2 carreras, 9 asignaturas, 6 semestres, 23 secciones, 37 inscripciones y 33 notas. Los datos históricos son coherentes entre sí y las secciones de semestres terminados quedan cerradas.

---

## 3. Modelo de datos: embedding y referencing

| Colección | Contenido principal | Forma de relación |
| --- | --- | --- |
| `users` | Credenciales, correo y rol | Documento independiente |
| `students` | Perfil académico y carrera | Referencia a `users` y `careers` |
| `professors` | Perfil del profesor | Referencia a `users` |
| `careers` | Carreras y plan académico | Referencias a asignaturas |
| `subjects` | Catálogo y prerrequisitos | Referencias entre asignaturas |
| `semesters` | Año, periodo y estado | Documento independiente |
| `sections` | Oferta académica y cupos | Referencias a asignatura, semestre y profesor |
| `enrollments` | Inscripción y su estado | Referencias a estudiante, sección y semestre |
| `grades` | Nota final de una inscripción | Referencias a inscripción, estudiante, asignatura y semestre |
| `certificados_notas` | Certificado académico preparado para lectura | Vista materializada por estudiante |
| `audit_logs` | Registro temporal de operaciones | Documentos eliminados automáticamente por TTL |

### Datos embebidos

- El horario y la sala se guardan dentro de cada sección porque siempre se consultan junto con ella.
- La inscripción guarda un resumen de las reglas académicas validadas al momento de inscribir, lo que deja evidencia de la decisión tomada.

### Datos referenciados

Las entidades académicas reutilizadas —estudiantes, profesores, asignaturas, semestres y secciones— mantienen referencias mediante `ObjectId`. Esto evita repetir información y permite modificar un catálogo sin actualizar muchas copias.

Las notas se guardan en una colección separada y se relacionan mediante `enrollmentId`, `studentId`, `subjectId` y `semesterId`. Esta decisión facilita:

- Registrar o corregir una nota sin reescribir todo el documento del estudiante.
- Construir reportes por asignatura y semestre.
- Mantener un historial que puede crecer con los años.
- Crear certificados mediante pipelines de agregación.

---

## 4. Validación de esquema (`$jsonSchema`)

El archivo `database/mongo/1_collections_and_schema.js` crea validadores estrictos para las colecciones. MongoDB rechaza documentos con tipos incorrectos, campos desconocidos o valores fuera de las reglas declaradas.

Ejemplos importantes:

- `grades.value` debe ser un número entre `1.0` y `7.0`.
- Una inscripción solo puede tener estado `ACTIVE`, `CANCELLED` o `COMPLETED`.
- Una inscripción cancelada debe registrar `cancelledAt`; una completada debe registrar `completedAt`.
- En una sección, `availableSeats` no puede ser mayor que `totalSeats`.
- La hora de inicio de una clase debe ser anterior a su hora de término.
- Las referencias académicas se guardan como `ObjectId`.
- `additionalProperties: false` impide guardar campos que no pertenecen al modelo.

Las reglas que necesitan consultar varias colecciones —por ejemplo, comprobar prerrequisitos, verificar que el semestre esté activo o revisar los cupos— no pueden resolverse solamente con `$jsonSchema`. Esas reglas se validan en el backend dentro de la transacción de inscripción.

También se corrigió el registro de notas para que el documento enviado por el backend coincida exactamente con el contrato de `grades`.

---

## 5. Estrategia de índices

Los índices se crean en `database/mongo/2_indexes.js`.

| Colección | Índice | Tipo | Objetivo |
| --- | --- | --- | --- |
| `users` | `uniq_user_email` | Único | Evitar correos repetidos y acelerar el login |
| `users` | `uniq_user_id` | Único | Mantener un identificador numérico irrepetible |
| `subjects` | `uniq_subject_code` | Único | Evitar códigos de asignatura duplicados |
| `subjects` | `text_subject_name` | Texto | Buscar asignaturas por palabras y relevancia |
| `semesters` | `uniq_semester_year_period` | Único compuesto | Evitar dos periodos iguales del mismo año |
| `sections` | `idx_section_subject_semester_status` | Compuesto | Consultar oferta académica por ramo, semestre y estado |
| `enrollments` | `uniq_student_section_semester` | Único compuesto | Impedir inscripciones duplicadas del mismo estudiante |
| `enrollments` | `idx_student_status` | Compuesto | Consultar rápidamente inscripciones activas, canceladas o terminadas |
| `grades` | `uniq_grade_enrollment` | Único | Mantener una sola nota final por inscripción |
| `grades` | `idx_subject_semester` | Compuesto | Acelerar reportes por asignatura y semestre |
| `audit_logs` | `ttl_audit_operation_date` | TTL de 90 días | Eliminar automáticamente registros de auditoría vencidos |

Los índices únicos actúan como una segunda barrera de seguridad. Aunque el backend valida los datos antes de escribir, MongoDB también impide que dos solicitudes simultáneas creen información duplicada.

---

## 6. Funcionalidades MongoDB implementadas

### 6.1 Inscripción transaccional ACID

`EnrollmentTransactionService` ejecuta la inscripción como una sola operación lógica. Antes de confirmar:

1. Comprueba que el estudiante exista y esté activo.
2. Comprueba que la sección esté abierta y pertenezca al semestre activo.
3. Revisa que estén aprobados los prerrequisitos de la asignatura.
4. Descuenta un cupo de forma atómica, evitando que dos personas tomen el último cupo.
5. Crea la inscripción o reactiva la inscripción cancelada que ya existía.

Si cualquiera de esos pasos falla, la transacción se revierte completa. No queda un cupo descontado sin inscripción ni una inscripción sin cupo.

La cancelación también es transaccional: cambia la inscripción a `CANCELLED` y devuelve exactamente un cupo. Una reinscripción posterior reutiliza el mismo documento, por lo que no genera duplicados.

### 6.2 Registro coherente de notas

Al registrar o actualizar una nota, el backend valida que la inscripción exista, que no esté cancelada y que el profesor tenga acceso a esa sección. La nota y el cambio de la inscripción a `COMPLETED` se confirman en la misma transacción.

### 6.3 Pipelines de agregación (`$group` y `$bucket`)

`GET /api/mongo/reports/pass-fail-rate` combina dos resultados:

- Un pipeline con `$group` calcula aprobados, reprobados, promedio y porcentajes por asignatura y semestre.
- Un pipeline con `$bucket` distribuye las notas en rangos: reprobadas, suficientes, buenas y destacadas.

Los parámetros `subjectId` y `semesterId` son opcionales y permiten filtrar el reporte.

### 6.4 Vista materializada y Change Streams

La colección `certificados_notas` mantiene un certificado ya preparado para cada estudiante:

- Al iniciar el backend, se reconstruyen los certificados existentes.
- `CertificateChangeStreamService` escucha inserciones y modificaciones en `grades`.
- Cuando cambia una nota, se recalcula únicamente el certificado afectado.
- El pipeline usa `$lookup`, `$group`, `$project` y `$merge` para consolidar el historial.

El script `database/mongo/4_certificates_merge.js` también permite reconstruir manualmente toda la vista materializada:

```bash
docker cp database/mongo/4_certificates_merge.js mongo_primary:/tmp/4_certificates_merge.js
docker exec mongo_primary mongosh \
  "mongodb://localhost:27017/academic_mongo?replicaSet=rs0" \
  /tmp/4_certificates_merge.js
```

### 6.5 Búsqueda de texto

`GET /api/mongo/subjects/search?q=programacion` consulta el índice `text_subject_name`. MongoDB busca por palabras en el nombre de la asignatura y ordena los resultados según su relevancia.

---

## 7. Documentación de la API

Base URL: `http://localhost:9090`

La autenticación se realiza con JWT. Primero se solicita un token:

```bash
curl -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@usach.cl","password":"1234"}'
```

Para consumir una ruta protegida se envía el token retornado:

```bash
curl http://localhost:9090/api/subjects \
  -H "Authorization: Bearer REEMPLAZAR_CON_EL_TOKEN"
```

### Endpoints principales

| Método | Endpoint | Acceso | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Público | Inicio de sesión y entrega del JWT |
| `GET` | `/api/subjects` | Autenticado | Catálogo de asignaturas |
| `GET` | `/api/mongo/subjects/search?q=` | Todos los roles | Búsqueda de texto por nombre |
| `GET` | `/api/sections/student/{studentId}` | Estudiante propio o admin | Secciones del horario del estudiante |
| `GET` | `/api/sections/professor/{professorId}` | Profesor propio o admin | Clases del profesor |
| `POST` | `/api/enrollments/enroll` | Estudiante propio o admin | Inscripción transaccional |
| `DELETE` | `/api/enrollments/{id}` | Estudiante propietario o admin | Cancela y restaura el cupo |
| `GET` | `/api/enrollments/student/{studentId}` | Estudiante propio o admin | Historial de inscripciones |
| `GET` | `/api/enrollments/section/{sectionId}` | Profesor de la sección o admin | Inscritos de una sección |
| `GET` | `/api/students/{studentId}/curriculum` | Estudiante propio o admin | Avance curricular por semestre |
| `GET` | `/api/grades/student/{studentId}` | Estudiante propio o admin | Notas del estudiante |
| `POST` | `/api/grades` | Profesor de la sección o admin | Registra una nota |
| `PUT` | `/api/grades/{id}` | Profesor de la sección o admin | Corrige una nota existente |
| `GET` | `/api/certificates/{studentId}` | Estudiante propio o admin | Certificado por identificador numérico de usuario |
| `GET` | `/api/mongo/reports/pass-fail-rate` | Profesor o admin | Reporte agregado y distribución de notas |
| `GET` | `/api/mongo/certificates/{studentObjectId}` | Profesor o admin | Certificado por `ObjectId` de MongoDB |
| `GET` | `/api/mongo/directory/students` | Profesor o admin | Directorio académico para seleccionar estudiantes |

La documentación completa y los contratos de cada petición se pueden revisar en Swagger UI.

### Ejemplo de reporte agregado

```json
{
  "bySubjectAndSemester": [
    {
      "subjectCode": "ALG1",
      "subjectName": "Álgebra 1",
      "semesterYear": 2026,
      "semesterPeriod": "1S",
      "totalGraded": 4,
      "approved": 3,
      "failed": 1,
      "approvalRate": 75.0,
      "failureRate": 25.0,
      "averageGrade": 4.8
    }
  ],
  "gradeDistribution": [
    {
      "label": "Reprobadas",
      "rangeStart": 1.0,
      "rangeEndExclusive": 4.0,
      "count": 1
    }
  ]
}
```

---

## 8. Funcionalidades por rol

| Rol | Funcionalidades principales |
| --- | --- |
| **Estudiante** | Consultar su perfil, malla, cursos, horario, notas y certificado; revisar inscripciones activas, completadas o canceladas; inscribir y cancelar asignaturas propias |
| **Profesor** | Consultar sus secciones, horario y estudiantes inscritos; registrar o corregir notas de sus propias clases; revisar reportes agregados y certificados académicos |
| **Administrador** | Administrar catálogos, carreras, asignaturas, semestres, secciones, estudiantes y profesores; consultar listados globales; operar inscripciones y acceder a reportes |

La API aplica controles de acceso además de ocultar opciones en la interfaz. Un estudiante no puede consultar los datos privados de otro, y un profesor no puede modificar notas de una sección que no le pertenece.

---

## 9. Estructura del repositorio

```text
academic-management-system/
├── docker-compose.yml
├── .env
├── README.md
├── database/
│   └── mongo/
│       ├── 0_init_replica_set.js
│       ├── 1_collections_and_schema.js
│       ├── 1_5_seed_users.js
│       ├── 2_indexes.js
│       ├── 3_seed_mongo.js
│       └── 4_certificates_merge.js
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/usach/cl/demo/
│       ├── config/          # MongoDB, JWT, CORS y seguridad
│       ├── controller/      # Endpoints REST generales y MongoDB
│       ├── dto/             # Objetos de entrada y salida
│       ├── model/           # Modelo académico y documentos MongoDB
│       ├── repository/      # Consultas, escrituras y agregaciones
│       └── service/         # Reglas de negocio, transacciones y Change Streams
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── src/
        ├── components/      # Navbar y protección de rutas
        ├── context/         # Sesión y autenticación
        ├── pages/           # Vistas de admin, estudiante, profesor y MongoDB
        ├── router/          # Rutas por rol
        └── services/        # Cliente HTTP de la API
```

---

## 10. Cumplimiento del enunciado y verificaciones

### 10.1 Requisitos del Grupo 2

| # | Requisito | Implementación |
| --- | --- | --- |
| 1 | Modelar historial de notas y justificar embedding/referencing | Colección referenciada `grades` y datos de horario/reglas embebidos donde corresponde |
| 2 | Implementar validación con `$jsonSchema` | Validadores estrictos en `1_collections_and_schema.js` |
| 3 | Inscripción ACID con control del último cupo | `EnrollmentTransactionService` con transacción, descuento atómico y rollback |
| 4 | Reporte con `$group` y `$bucket` | `ReportAggregationService` y endpoint `/api/mongo/reports/pass-fail-rate` |
| 5 | Índices compuesto único y de texto | `uniq_student_section_semester` y `text_subject_name`, entre otros |
| 6 | Certificado materializado con `$merge` y Change Streams | `CertificateChangeStreamService` y colección `certificados_notas` |
| 7 | Replica Set con primario y secundario | Servicios `mongo-primary`, `mongo-secondary` y `mongo-init` |
| 8 | API JSON protegida con JWT y roles | Spring Security, filtro JWT y autorización por propietario/rol |
| 9 | Interfaz web integrada | Flujos F1 de inscripción y F2 de reportes, búsqueda y certificados |

### 10.2 Comprobaciones funcionales realizadas

La versión integrada fue revisada con datos correctos e incorrectos. Entre los escenarios comprobados se encuentran:

- Construcción correcta del backend y del frontend.
- Inicio desde una base limpia y respuesta `HTTP 200` de OpenAPI.
- Consistencia del mock: referencias válidas, cupos coincidentes y notas asociadas a inscripciones reales.
- Rechazo de inscripciones duplicadas, sin cupo, con semestre cerrado o sin prerrequisitos.
- Competencia por el último cupo: solo una solicitud puede obtenerlo.
- Cancelación con devolución del cupo y reinscripción sin duplicar documentos.
- Rechazo de notas fuera del rango permitido o asociadas a inscripciones canceladas.
- Actualización del certificado después de registrar o modificar una nota.
- Funcionamiento del reporte, sus filtros, la distribución por rangos y la búsqueda de texto.
- Respuestas `401` sin autenticación y `403` cuando el rol o propietario no corresponde.

---

## 11. División de trabajo del Laboratorio 3

| Parte | Responsabilidad | Resultado integrado |
| --- | --- | --- |
| **B1 — Mongo Core** | Conexión, documentos, validadores, transacción ACID y endpoints de inscripción | Base MongoDB operativa y flujo seguro de inscripción, cancelación y reinscripción |
| **B2 — Mongo Analytics** | Pipelines, índices, Change Streams, vista materializada y mock | Reportes, búsqueda, certificados reactivos y escenario de demostración coherente |
| **F1 — Enrollment UI** | Interfaz del proceso de inscripción | Consulta de oferta, inscripción, cancelación e historial por estado |
| **F2 — Reports UI** | Interfaz de reportes, certificados y búsqueda | Visualización de indicadores, filtros, certificado académico y buscador de asignaturas |

Las cuatro partes trabajan sobre la misma API y la misma base Full MongoDB. La integración final conserva la separación por responsabilidades, pero presenta una experiencia única para cada rol.
