# Sistema de Administración Académica Universitaria

Sistema de información multi-capa para la gestión académica universitaria. Permite administrar estudiantes, asignaturas, inscripciones, calificaciones y reportes, con autenticación basada en JWT y control de acceso por roles (ADMIN, PROFESOR, ESTUDIANTE). Desarrollado como parte del Laboratorio 1 de Taller de Base de Datos.

---

## 1. Arquitectura y tecnologías utilizadas

### Arquitectura general
- **Frontend**: Aplicación React de una sola página (SPA) que consume la API REST.
- **Backend**: API REST desarrollada con Spring Boot, lógica de negocio delegada parcialmente a la base de datos mediante procedimientos almacenados y triggers.
- **Base de datos**: PostgreSQL con objetos avanzados (triggers, stored procedures, vistas materializadas, índices).

### Tecnologías principales

| Capa           | Tecnologías / librerías                                                                |
|----------------|----------------------------------------------------------------------------------------|
| Backend        | Java 17, Spring Boot 3.x, Spring Security, Spring Data JDBC, JWT (jjwt), Lombok        |
| Documentación  | springdoc-openapi (Swagger UI)                                                         |
| Base de datos  | PostgreSQL 15+                                                                         |
| Frontend       | React 18, React Router DOM, Axios, Bootstrap 5                                         |
| Contenedores   | Docker, Docker Compose                                                                 |
| Herramientas   | pgAdmin 4 (administración de BD)                                                       |

---

## 2. Manual de instalación y despliegue

### 2.1 Requisitos previos
- [Docker](https://www.docker.com/products/docker-desktop/) instalado y en ejecución (Docker Desktop en Windows/Mac, o Docker Engine en Linux).
- [pgAdmin 4](https://www.pgadmin.org/download/) (opcional, pero necesario para la carga inicial de scripts).

### 2.2 Pasos para ejecutar el proyecto

1. **Clonar o descargar el repositorio**

   git clone https://github.com/vicente-tapia07/academic-management-system.git
   cd TBD-Lab1

2. **Construir las imágenes Docker**

   docker-compose build

3. **Levantar los contenedores**

   docker-compose up

4. **Cargar los scripts de base de datos**
   - Abre pgAdmin 4.
   - Registra un nuevo servidor (Servers → Register → Server):
     - *General → Name*: cualquier nombre (ej. `LabBD`).
     - *Connection → Host name/address*: `localhost`
     - *Port*: `5433`
     - *Maintenance database*: `postgres`
     - *Username*: `postgres`
     - *Password*: `123`
   - Conéctate a la base de datos `TBDLab1`.
   - Abre el **Query Tool** y ejecuta los scripts en el siguiente orden:
     1. `db_schema.sql`   → creación de tablas y relaciones.
     2. `db_mock.sql`     → inserción de datos de prueba (seeders).
     3. `db_sp.sql`       → procedimientos almacenados.
     4. `db_triggers.sql` → triggers y vista materializada.

### 2.3 Acceso a la aplicación

- **Frontend**: [http://localhost:3000](http://localhost:3000) (React).
- **Backend (API)**: [http://localhost:9090](http://localhost:9090).
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (documentación interactiva).

### 2.4 Credenciales de prueba

| Rol         | Email                | Contraseña |
|-------------|----------------------|------------|
| ADMIN       | admin@usach.cl       | 1234       |
| ESTUDIANTE  | juan@usach.cl        | 1234       |
| PROFESOR    | carlos.ruiz@usach.cl | 1234       |

---

## 3. Documentación de la API

Todos los endpoints están protegidos con JWT, excepto el de login. Incluye el token en la cabecera `Authorization: Bearer <token>`.

### 3.1 Autenticación

**POST /api/auth/login**

- Request body:
  ```json
  {
    "email": "admin@usach.cl",
    "password": "1234"
  }
  ```
- Respuesta exitosa (200 OK):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB1c2FjaC5jbCIsImlkIjozLCJyb2xlcyI6WyJST0xFX0FETUlOIl0sImlhdCI6MTc3ODAyMDYyOSwiZXhwIjoxNzc4MTA3MDI5fQ.UnG1nNmVcGzYaucLQ4faB17cy9kF4dUaalaZb04XDMg"
  }
  ```

### 3.2 Endpoints principales

| Método | Endpoint                             | Roles autorizados       | Descripción breve                                                                 |
|--------|--------------------------------------|-------------------------|-----------------------------------------------------------------------------------|
| POST   | `/api/auth/login`                    | Público                 | Inicia sesión y retorna JWT.                                                     |
| GET    | `/api/students`                      | ADMIN                   | Lista todos los estudiantes.                                                     |
| GET    | `/api/students/{id}`                 | ADMIN / propio estudiante | Obtiene los datos de un estudiante específico.                                   |
| GET    | `/api/students/{id}/curriculum`      | ESTUDIANTE (propio)     | Malla curricular con estado por asignatura (Aprobada, Reprobada, Cursando).      |
| POST   | `/api/students`                      | ADMIN                   | Crea un nuevo estudiante.                                                        |
| PUT    | `/api/students/{id}`                 | ADMIN                   | Actualiza datos del estudiante.                                                  |
| DELETE | `/api/students/{id}`                 | ADMIN                   | Elimina un estudiante.                                                           |
| POST   | `/api/enrollments/enroll`            | ESTUDIANTE              | Inscribe al estudiante en una sección (resta cupo, valida prerrequisitos).       |
| GET    | `/api/enrollments/student/{id}`      | ESTUDIANTE (propio)     | Lista inscripciones del estudiante.                                              |
| POST   | `/api/semesters/{id}/close`          | ADMIN                   | Ejecuta cierre de semestre (congela notas, calcula PPA).                         |
| GET    | `/api/subjects`                      | Todos autenticados      | Lista todas las asignaturas.                                                     |
| POST   | `/api/subjects`                      | ADMIN                   | Crea una nueva asignatura.                                                       |
| PUT    | `/api/subjects/{id}`                 | ADMIN                   | Actualiza una asignatura.                                                        |
| DELETE | `/api/subjects/{id}`                 | ADMIN                   | Elimina una asignatura.                                                          |
| GET    | `/api/professors/{id}/sections`      | PROFESOR (propio)       | Lista las secciones asignadas al profesor.                                     |
| POST   | `/api/grades`                        | PROFESOR                | Ingresa/actualiza una calificación (solo dentro del calendario académico).       |
| GET    | `/api/grades`                        | Todos (filtrado por rol)| Lista calificaciones según el rol (profesor ve sus asignaturas, estudiante las propias). |
| GET    | `/api/reports/failure-rate`          | ADMIN, PROFESOR         | Reporte de tasa de reprobación histórica por asignatura (vista materializada).  |

### 3.3 Ejemplos de solicitudes y respuestas

#### Obtener todos los estudiantes (ADMIN)
GET `/api/students`

Response:
```json
[
  {
    "id": 1,
    "usuarioId": 1,
    "enrollmentNumber": "2024001",
    "firstName": "Juan",
    "lastName": "Perez",
    "academicStatus": "ACTIVE"
  }
]
```

#### Malla curricular de un estudiante
GET `/api/students/1/curriculum`

Response (estructura esperada):
```json
[
  {
    "subjectCode": "CAL1",
    "subjectName": "Cálculo 1",
    "status": "APROBADA"
  },
  ...
]
```

#### Reporte de reprobación (vista materializada)
GET `/api/reports/failure-rate`

Response:
```json
[
  {
    "subjectId": 1,
    "subjectCode": "CAL1",
    "subjectName": "Cálculo 1",
    "totalGrades": 2,
    "failedGrades": 1,
    "failurePercentage": 50.0
  },
  ...
]
```

---

## 4. Estructura del proyecto

```
├── backend/                # Proyecto Spring Boot (API REST)
│   ├── src/main/java/...
│   └── pom.xml
├── frontend/               # Aplicación React
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── context/
│   │   └── App.jsx
│   └── package.json
├── database/               # Scripts SQL
│   ├── db_schema.sql
│   ├── db_mock.sql
│   ├── db_sp.sql
│   └── db_triggers.sql
├── docker-compose.yml
└── README.md
```