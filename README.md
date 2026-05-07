# Sistema de Administración Académica Universitaria

Sistema de información multi-capa para la gestión académica universitaria. Permite administrar estudiantes, asignaturas, inscripciones, calificaciones y reportes, con autenticación basada en JWT y control de acceso por roles (ADMIN, PROFESOR, ESTUDIANTE). Desarrollado como parte del Laboratorio 1 de Taller de Base de Datos.

---

## 1. Arquitectura y tecnologías utilizadas

### Arquitectura general

- **Frontend**: Aplicación React de una sola página (SPA) que consume la API REST.
- **Backend**: API REST desarrollada con Spring Boot, sin ORM. La lógica de negocio está delegada parcialmente a la base de datos mediante procedimientos almacenados y triggers.
- **Base de datos**: PostgreSQL con objetos avanzados (triggers, stored procedures, vistas materializadas, índices).

### Tecnologías principales

| Capa          | Tecnologías / librerías                                                            |
|---------------|------------------------------------------------------------------------------------|
| Backend       | Java 21, Spring Boot 4.0.6, Spring Security, Spring Data JDBC, JWT (jjwt), Lombok |
| Documentación | springdoc-openapi (Swagger UI)                                                     |
| Base de datos | PostgreSQL 15+                                                                     |
| Frontend      | React 18, React Router DOM, Axios, Bootstrap 5                                     |
| Contenedores  | Docker, Docker Compose                                                             |

---

## 2. Manual de instalación y despliegue

### 2.1 Requisitos previos

Instalar las siguientes herramientas antes de continuar:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — para correr el backend y el frontend.
- [pgAdmin 4](https://www.pgadmin.org/download/) — para cargar los scripts de base de datos.
- Git — para clonar el repositorio.

> **No se requiere** tener Java ni Node.js instalados localmente. Docker los provee.

---

### 2.2 Paso 1 — Clonar el repositorio

Abrir una terminal (CMD, PowerShell o terminal de Linux) y ejecutar:

```bash
git clone https://github.com/vicente-tapia07/academic-management-system.git
cd academic-management-system
```

---

### 2.3 Paso 2 — Levantar los contenedores Docker

Dentro de la carpeta del proyecto, ejecutar:

```bash
docker-compose build
docker-compose up
```

> El primer `build` puede tardar varios minutos porque descarga las imágenes base. Solo ocurre la primera vez.

Esperar hasta ver en la consola mensajes como estos:

```
db_1       | database system is ready to accept connections
backend_1  | Started DemoApplication in X seconds
frontend_1 | Compiled successfully
```

> **Importante:** dejar esta terminal abierta mientras se usa el sistema. Cerrarla detiene todos los contenedores.

En este punto el motor de PostgreSQL está corriendo dentro de Docker en el puerto `5433`, pero la base de datos aún está vacía. Los siguientes pasos la crean y cargan los datos.

---

### 2.4 Paso 3 — Registrar el servidor en pgAdmin 4

Como la base de datos corre dentro de un contenedor Docker, pgAdmin (que está instalado en tu computador) debe conectarse a ella a través del puerto que Docker expone hacia el exterior.

1. Abrir **pgAdmin 4**.

2. En el panel izquierdo, hacer clic derecho sobre **Servers** → **Register** → **Server...**.

3. En la pestaña **General**:
   - **Name**: `TBD - Docker` (o cualquier nombre descriptivo).

4. En la pestaña **Connection**:
   - **Host name/address**: `localhost`
   - **Port**: `5433` ← importante, no usar el 5432 por defecto.
   - **Maintenance database**: `postgres`
   - **Username**: `postgres`
   - **Password**: `123`

5. Hacer clic en **Save**.

Si la conexión es exitosa, el servidor `TBD - Docker` aparece en el panel izquierdo sin errores ni candados rojos.

---

### 2.5 Paso 4 — Crear la base de datos

El motor de PostgreSQL está listo, pero el espacio de trabajo del proyecto aún no existe. Hay que crearlo manualmente:

1. En pgAdmin, desplegar el servidor **TBD - Docker** recién registrado.

2. Hacer clic derecho sobre **Databases** → **Create** → **Database...**.

3. En el campo **Database** escribir exactamente:
   ```
   TBDLab1
   ```
   > Este nombre es obligatorio y sensible a mayúsculas. El backend lo busca con ese nombre exacto. Si se escribe diferente (ej. `tbdlab1` o `TbdLab1`), el backend no podrá conectarse.

4. Hacer clic en **Save**.

La base de datos `TBDLab1` aparece en la lista bajo Databases. En este momento está completamente vacía, sin tablas ni datos.

---

### 2.6 Paso 5 — Cargar los scripts SQL (paso crítico)

Aquí se crean todas las tablas, los stored procedures, los triggers, la vista materializada, los índices y los datos de prueba. Los archivos deben ejecutarse en el **orden exacto indicado**.

1. En pgAdmin, hacer clic derecho sobre la base de datos **TBDLab1** → **Query Tool**.

2. Se abre el editor de consultas SQL. En la barra de herramientas superior, hacer clic en el **ícono de carpeta** 📁 (Open File).

3. Navegar hasta la carpeta `database/` dentro del proyecto clonado. La ruta depende de dónde se clonó:
   - Windows: `C:\Users\TuUsuario\academic-management-system\database\`
   - Linux/Mac: `~/academic-management-system/database/`

4. Ejecutar los archivos uno por uno en este orden. Para cada uno:
   - Abrirlo con el ícono de carpeta 📁.
   - Hacer clic en el **ícono del rayo ▶** (Execute) o presionar **F5**.
   - Esperar a que aparezca el mensaje `Query returned successfully` en el panel inferior.
   - **Recién entonces** abrir el siguiente archivo.

| Orden | Archivo         | Qué contiene                                                                                         |
|-------|-----------------|------------------------------------------------------------------------------------------------------|
| 1°    | `db_schema.sql` | Creación de todas las tablas y relaciones. También incluye los índices, los dos triggers, los dos stored procedures (`sp_close_semester` y `sp_enroll_student`) y la vista materializada `mv_failure_rate`. |
| 2°    | `db_mock.sql`   | Datos de prueba: usuarios (admin, estudiantes, profesores), carreras, asignaturas, secciones, inscripciones y notas del semestre cerrado. |

> **¿Por qué este orden?** El `db_mock.sql` inserta filas en tablas que deben existir previamente. Si se ejecuta antes que el schema, fallará con errores de tabla no encontrada.

> **¿Dónde están los triggers y stored procedures?** Todo está consolidado dentro de `db_schema.sql`. No hay archivos separados para triggers ni procedures.

---

### 2.7 Paso 6 — Verificar que el sistema funciona

Con los contenedores corriendo y los scripts cargados, abrir el navegador y verificar:

| Verificación  | URL                                       | Resultado esperado                        |
|---------------|-------------------------------------------|-------------------------------------------|
| Frontend      | http://localhost:3000                     | Pantalla de login del sistema             |
| Backend       | http://localhost:9090                     | Respuesta del servidor (puede ser error JSON, es normal) |
| Swagger UI    | http://localhost:9090/swagger-ui.html     | Documentación interactiva de la API       |

Luego iniciar sesión con cada credencial para confirmar que los datos del mock cargaron correctamente:

| Rol        | Email                | Contraseña | Qué debe verse                  |
|------------|----------------------|------------|---------------------------------|
| ADMIN      | admin@usach.cl       | 1234       | Dashboard de administración     |
| ESTUDIANTE | juan@usach.cl        | 1234       | Dashboard del estudiante        |
| PROFESOR   | carlos.ruiz@usach.cl | 1234       | Vista del profesor              |

---

### 2.8 Configuración de puertos mediante archivo `.env`

Si alguno de los puertos por defecto está ocupado en tu computador, puedes cambiarlos sin tocar el `docker-compose.yml`. El proyecto usa un archivo `.env` para centralizar esta configuración.

En la raíz del proyecto existe un archivo llamado `.env` con este contenido por defecto:

```env
# Puerto externo de PostgreSQL (el que usa pgAdmin para conectarse)
DB_PORT=5433

# Puerto externo del backend (el que usa el frontend y Swagger)
BACKEND_PORT=9090

# Puerto externo del frontend
FRONTEND_PORT=3000
```

**¿Cómo cambiar un puerto?**

1. Abrir el archivo `.env` con cualquier editor de texto (Notepad, VS Code, etc.).
2. Cambiar el número del puerto que esté ocupado. Por ejemplo, si el puerto `5433` está en uso:
   ```env
   DB_PORT=5434
   ```
3. Guardar el archivo.
4. Volver a ejecutar:
   ```bash
   docker-compose down
   docker-compose up
   ```
5. Actualizar la conexión en pgAdmin con el nuevo puerto (`5434` en el ejemplo).

> **Importante:** si cambias `BACKEND_PORT` o `FRONTEND_PORT`, también debes actualizar la URL base en el frontend (`src/services/api.js`) para que apunte al puerto correcto.

---

### 2.9 Solución de problemas frecuentes

**El puerto 5433 aparece como ocupado al hacer `docker-compose up`**

Otro proceso en el computador está usando ese puerto. Cambiar el valor en el archivo `.env`:

```env
DB_PORT=5434
```

Luego en pgAdmin conectarse al puerto `5434` en vez de `5433`.

**pgAdmin no puede conectarse al servidor Docker**

Verificar que el contenedor de base de datos esté corriendo:

```bash
docker-compose ps
```

El contenedor `db` debe aparecer con estado `Up`. Si aparece `Exit`, revisar los logs:

```bash
docker-compose logs db
```

**El backend muestra error de conexión a la base de datos**

Verificar que los scripts ya se ejecutaron en pgAdmin y que la base de datos se llama exactamente `TBDLab1`. Luego reiniciar el backend:

```bash
docker-compose restart backend
```

**Para detener todos los contenedores:**

```bash
docker-compose down
```

**Para hacer un reset completo (borra todos los datos y vuelve a empezar):**

```bash
docker-compose down -v
docker-compose up
```

Luego repetir el Paso 5 (cargar los scripts en pgAdmin desde cero).

---

## 3. Documentación de la API

Todos los endpoints están protegidos con JWT, excepto el de login. Incluir el token en la cabecera de cada request:

```
Authorization: Bearer <token>
```

### 3.1 Autenticación

**POST /api/auth/login**

Request body:
```json
{
  "email": "admin@usach.cl",
  "password": "1234"
}
```

Respuesta exitosa (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 3.2 Tabla de endpoints

| Método | Endpoint                        | Roles autorizados           | Descripción                                                                  |
|--------|---------------------------------|-----------------------------|------------------------------------------------------------------------------|
| POST   | `/api/auth/login`               | Público                     | Inicia sesión y retorna JWT.                                                 |
| GET    | `/api/students`                 | ADMIN                       | Lista todos los estudiantes.                                                 |
| GET    | `/api/students/{id}`            | ADMIN, PROFESOR, ESTUDIANTE | Obtiene datos de un estudiante.                                              |
| GET    | `/api/students/{id}/curriculum` | ADMIN, ESTUDIANTE           | Malla curricular con estado por asignatura (Aprobada, Reprobada, Cursando). |
| POST   | `/api/students`                 | ADMIN                       | Crea un nuevo estudiante.                                                    |
| PUT    | `/api/students/{id}`            | ADMIN                       | Actualiza datos del estudiante.                                              |
| DELETE | `/api/students/{id}`            | ADMIN                       | Elimina un estudiante.                                                       |
| POST   | `/api/enrollments/enroll`       | ADMIN                       | Inscribe al estudiante en una sección (stored procedure, valida prerrequisitos via trigger). |
| GET    | `/api/enrollments/student/{id}` | ADMIN, PROFESOR, ESTUDIANTE | Lista inscripciones del estudiante con nombre de asignatura y profesor.     |
| GET    | `/api/subjects`                 | Todos autenticados          | Lista todas las asignaturas.                                                 |
| POST   | `/api/subjects`                 | ADMIN                       | Crea una nueva asignatura.                                                   |
| PUT    | `/api/subjects/{id}`            | ADMIN                       | Actualiza una asignatura.                                                    |
| DELETE | `/api/subjects/{id}`            | ADMIN                       | Elimina una asignatura.                                                      |
| GET    | `/api/professors`               | Todos autenticados          | Lista todos los profesores.                                                  |
| GET    | `/api/professors/{id}`          | Todos autenticados          | Obtiene un profesor por su ID.                                               |
| POST   | `/api/professors`               | ADMIN                       | Crea un nuevo profesor.                                                      |
| PUT    | `/api/professors/{id}`          | ADMIN                       | Actualiza datos de un profesor.                                              |
| DELETE | `/api/professors/{id}`          | ADMIN                       | Elimina un profesor.                                                         |
| POST   | `/api/professors/grade`         | ADMIN, PROFESOR             | Ingresa una calificación (el trigger bloquea si está fuera del calendario). |
| GET    | `/api/professors/reports`       | ADMIN, PROFESOR             | Reporte de tasa de reprobación por asignatura (vista materializada).        |
| POST   | `/api/semesters/{id}/close`     | ADMIN                       | Cierra el semestre (stored procedure: calcula promedio ponderado, bloquea estudiantes reprobados). |

### 3.3 Ejemplos de solicitudes y respuestas

#### Lista de estudiantes
```
GET /api/students
Authorization: Bearer <token_admin>
```
```json
[
  {
    "id": 1,
    "usuarioId": 1,
    "enrollmentNumber": "2024001",
    "firstName": "Juan",
    "lastName": "Pérez",
    "academicStatus": "ACTIVE"
  }
]
```

#### Malla curricular de un estudiante
```
GET /api/students/1/curriculum
Authorization: Bearer <token>
```
```json
[
  {
    "subjectId": 1,
    "subjectCode": "CAL1",
    "subjectName": "Cálculo 1",
    "credits": 4,
    "status": "APPROVED",
    "grade": 6.0
  },
  {
    "subjectId": 2,
    "subjectCode": "CAL2",
    "subjectName": "Cálculo 2",
    "credits": 4,
    "status": "ENROLLED",
    "grade": null
  }
]
```

#### Subir una nota (PROFESOR)
```
POST /api/professors/grade?professorRut=11222333-4
Authorization: Bearer <token_profesor>
Content-Type: application/json
```
```json
{
  "enrollmentId": 6,
  "value": 5.5,
  "entryDate": "2025-06-15"
}
```

> Si la fecha está fuera del calendario académico del semestre, el **Trigger 2** bloquea la operación y retorna error con el mensaje: `"Fuera del calendario académico de notas"`.

#### Reporte de reprobación (vista materializada)
```
GET /api/professors/reports
Authorization: Bearer <token_profesor_o_admin>
```
```json
[
  {
    "subjectId": 1,
    "subjectCode": "CAL1",
    "subjectName": "Cálculo 1",
    "totalGrades": 3,
    "failedGrades": 1,
    "failurePercentage": 33.33
  }
]
```

#### Cierre de semestre (ADMIN)
```
POST /api/semesters/1/close
Authorization: Bearer <token_admin>
```
> Ejecuta el stored procedure `sp_close_semester`. Calcula el promedio ponderado por créditos de cada estudiante. Si el promedio es menor a 4.0, cambia el `academic_status` a `BLOCKED` y cierra el semestre.

---

## 4. Objetos de base de datos implementados

### Triggers

| Nombre                       | Tabla        | Cuándo se activa     | Función                                                                  |
|------------------------------|--------------|----------------------|--------------------------------------------------------------------------|
| `trg_check_prerequisites`    | `enrollment` | BEFORE INSERT        | Bloquea la inscripción si el estudiante no aprobó los prerrequisitos.   |
| `trg_check_calendario_notas` | `grade`      | BEFORE INSERT/UPDATE | Impide ingresar notas fuera del calendario académico del semestre.      |

### Stored Procedures

| Nombre              | Descripción                                                                                   |
|---------------------|-----------------------------------------------------------------------------------------------|
| `sp_close_semester` | Cierra el semestre: calcula promedio ponderado y bloquea estudiantes con promedio < 4.0.     |
| `sp_enroll_student` | Inscribe a un estudiante en una sección en una transacción atómica, descontando un cupo.    |

### Vista materializada

| Nombre            | Descripción                                                                    |
|-------------------|--------------------------------------------------------------------------------|
| `mv_failure_rate` | Tasa de reprobación histórica por asignatura. Se refresca al consultar el reporte. |

### Índices

| Índice                   | Tabla / Columna              | Propósito                          |
|--------------------------|------------------------------|------------------------------------|
| `idx_usuario_rut`        | `usuario(rut)`               | Búsqueda rápida por RUT.           |
| `idx_student_enrollment` | `student(enrollment_number)` | Búsqueda por número de matrícula.  |
| `idx_subject_code`       | `subject(code)`              | Búsqueda por código de asignatura. |

---

## 5. Estructura del repositorio

```
├── backend/                # Proyecto Spring Boot (API REST)
│   ├── src/main/java/usach/cl/demo/
│   │   ├── config/         # JWT, Spring Security
│   │   ├── controller/     # Endpoints REST
│   │   ├── dto/            # Objetos de transferencia de datos
│   │   ├── model/          # Entidades
│   │   ├── repository/     # Acceso a BD con JdbcTemplate / JdbcClient
│   │   └── service/        # Lógica de negocio
│   └── pom.xml
├── frontend/               # Aplicación React
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   ├── router/
│   │   └── services/
│   └── package.json
├── database/               # Scripts SQL
│   ├── db_schema.sql       # Tablas, relaciones, índices, triggers, stored procedures y vista materializada
│   └── db_mock.sql         # Datos de prueba (usuarios, estudiantes, profesores, notas)
├── docker-compose.yml
└── README.md
└── .env
```
