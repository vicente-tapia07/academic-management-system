# Sistema de Administración Académica Universitaria

Sistema de información multi-capa para la gestión académica universitaria. Permite administrar estudiantes, asignaturas, inscripciones, calificaciones y reportes, con autenticación basada en JWT y control de acceso por roles (ADMIN, PROFESSOR, STUDENT). Desarrollado como parte del Laboratorio 1 de Taller de Base de Datos.

---

## 1. Arquitectura y tecnologías utilizadas

### Arquitectura general

- **Frontend**: Aplicación React de una sola página (SPA) que consume la API REST.
- **Backend**: API REST desarrollada con Spring Boot, sin ORM. La lógica de negocio está delegada parcialmente a la base de datos mediante procedimientos almacenados y triggers.
- **Base de datos**: PostgreSQL con objetos avanzados (triggers, stored procedures, vista materializada, índices).

### Tecnologías principales

| Capa          | Tecnologías / librerías                                                            |
|---------------|--------------------------------------------------------------------------------------|
| Backend       | Java 21, Spring Boot 4.0.6, Spring Security, Spring Data JDBC, JWT (jjwt), Lombok    |
| Documentación | springdoc-openapi (Swagger UI)                                                       |
| Base de datos | PostgreSQL 15                                                                        |
| Frontend      | React 18, React Router DOM, Axios, Bootstrap 5                                      |
| Contenedores  | Docker, Docker Compose                                                              |

---

## 2. Manual de instalación y despliegue

El sistema está completamente contenerizado. **No es necesario instalar Java, Node.js ni PostgreSQL en tu máquina.** Docker se encarga de todo, incluyendo la creación de la base de datos y la carga de los datos de prueba.

Las instrucciones son las mismas para Windows, Linux y Mac. Las únicas diferencias puntuales (terminal a usar, y un comando alternativo si `docker-compose` no es reconocido) están marcadas explícitamente donde corresponde.

### 2.1 Requisitos previos

- **Git** instalado en el sistema.
- **Docker Desktop** instalado y **corriendo** (el ícono de la ballena debe estar activo en la barra de tareas / bandeja del sistema).
  - Descarga: https://www.docker.com/products/docker-desktop/
  - **Windows:** Docker Desktop requiere WSL2 (Windows Subsystem for Linux). El instalador lo configura automáticamente en la mayoría de los casos; si pide reiniciar el equipo durante la instalación, hacerlo. Si al abrir Docker te arroja un aviso de que WSL no está instalado, abre una terminal de PowerShell como Administrador, ejecuta el siguiente comando y **reinicia tu computadora** obligatoriamente:
   ```powershell
   wsl --install

> **Windows:** usar **PowerShell** (viene incluido en Windows) o la terminal de Git Bash (se instala junto con Git). No usar el CMD clásico, algunos comandos de este manual no son compatibles con él.

---

### 2.2 Paso 1 — Clonar el repositorio

**Linux / Mac (terminal) y Windows (PowerShell o Git Bash):**

```bash
git clone https://github.com/vicente-tapia07/academic-management-system.git
cd academic-management-system
```

El archivo `.env` con la configuración de puertos y credenciales ya viene incluido en el repositorio — no es necesario crearlo ni copiarlo manualmente, en ningún sistema operativo.

---

### 2.3 Paso 2 — Levantar el sistema completo

Dentro de la carpeta del proyecto, ejecutar:

```bash
docker-compose up --build
```

> **Si el comando no es reconocido** (`docker-compose: command not found` o `no se reconoce como un comando`): tu instalación de Docker usa la sintaxis nueva integrada. Usar en su lugar:
> ```bash
> docker compose up --build
> ```
> (sin guion, con espacio). Ambas formas son equivalentes — cambia solo según qué versión de Docker esté instalada. Esto aplica igual en Windows, Linux y Mac.

Este único comando:

1. Construye la imagen del backend (Spring Boot).
2. Construye la imagen del frontend (React).
3. Levanta PostgreSQL.
4. **Crea automáticamente la base de datos y carga las tablas, triggers, stored procedures, la vista materializada y los datos de prueba** — no requiere ninguna acción manual en pgAdmin ni en ningún otro cliente de base de datos.

> El primer `build` puede tardar varios minutos porque descarga las imágenes base. Las siguientes veces es mucho más rápido gracias al cache de Docker.

Esperar hasta ver en la consola mensajes como estos:

```
postgres_db | database system is ready to accept connections
backend_tbd | Started DemoApplication in X seconds
frontend_tbd | Compiled successfully!
```

> **Importante:** dejar esta terminal abierta mientras se usa el sistema, o agregar `-d` al final del comando (`docker-compose up --build -d`) para correrlo en segundo plano y recuperar el control de la terminal.

---

### 2.4 Paso 3 — Verificar que el sistema funciona

Con los contenedores corriendo, abrir el navegador y verificar:

| Verificación | URL                                    | Resultado esperado                                        |
|---------------|-----------------------------------------|-------------------------------------------------------------|
| Frontend      | http://localhost:3000                   | Pantalla de login del sistema                                |
| Backend       | http://localhost:9090                   | Respuesta del servidor (puede ser error JSON, es normal)     |
| Swagger UI    | http://localhost:9090/swagger-ui/index.html | Documentación interactiva de la API                       |

Luego iniciar sesión con cada credencial para confirmar que los datos de prueba cargaron correctamente:

| Rol       | Email           | Contraseña | Qué debe verse               |
|-----------|-----------------|------------|-------------------------------|
| ADMIN     | admin@usach.cl  | 1234       | Dashboard de administración   |
| STUDENT   | juan@usach.cl   | 1234       | Dashboard del estudiante      |
| PROFESSOR | carlos@usach.cl | 1234       | Vista del profesor            |

---

### 2.5 Configuración de puertos mediante el archivo `.env`
En la raíz del proyecto existe un archivo `.env` encargado de orquestar las credenciales y puertos de los contenedores. 

> 🟥 **Atención — Coincidencia de puertos interna:** Para asegurar el correcto funcionamiento del Backend en entornos cerrados, la variable `BACKEND_CONTAINER_PORT` debe apuntar **obligatoriamente al puerto 9090**. Esto es debido a que el servidor embebido Tomcat en Spring Boot y el `EXPOSE` de su respectivo Dockerfile están parametrizados nativamente bajo el puerto `9090`. Modificar este valor interno a `8080` u otro romperá el puente de comunicación del tráfico.

```env
BACKEND_HOST_PORT=9090
BACKEND_CONTAINER_PORT=9090
FRONTEND_HOST_PORT=3000
DB_HOST_PORT=5433

POSTGRES_USER=postgres
POSTGRES_PASSWORD=123
POSTGRES_DB=academic_db

REACT_APP_API_URL=http://localhost:9090
```

**¿Cómo cambiar un puerto si está ocupado?**

1. Editar el valor correspondiente en `.env` con cualquier editor de texto (Notepad, VS Code, nano, etc. — el archivo es el mismo en cualquier sistema operativo).
2. Si se cambia `BACKEND_HOST_PORT`, actualizar también `REACT_APP_API_URL` con el nuevo puerto.
3. Guardar y volver a levantar:
   ```bash
   docker-compose down
   docker-compose up --build
   ```

---

### 2.6 Solución de problemas frecuentes

**Un puerto aparece como ocupado al hacer `docker-compose up`**

Cambiar el valor correspondiente en `.env` (ver sección 2.5) y volver a levantar los contenedores.

> **Windows:** el puerto `5432`/`5433` puede aparecer ocupado si tienes PostgreSQL instalado nativamente (fuera de Docker) y corriendo como servicio de Windows. Se soluciona igual, cambiando `DB_HOST_PORT` en el `.env` — no es necesario desinstalar nada.

**El backend no puede conectarse a la base de datos**

Verificar que el contenedor `postgres_db` esté con estado `Up`:

```bash
docker-compose ps
```

Si aparece `Exit`, revisar los logs:

```bash
docker-compose logs db
```

**Docker Desktop no arranca o los comandos `docker` no responden (Windows)**

Verificar que Docker Desktop esté efectivamente abierto y con el ícono de la ballena estable en la bandeja del sistema (no parpadeando). Si Docker Desktop pide habilitar WSL2 o reiniciar, hacerlo y volver a intentar.

**Necesito reiniciar todo desde cero (borra todos los datos y vuelve a cargar los scripts)**

```bash
docker-compose down -v
docker-compose up --build
```

**Error: "unable to get image... failed to connect to the docker API / El sistema no puede encontrar el archivo especificado"**
* **Por qué ocurre:** Intentaste ejecutar `docker compose` mientras la aplicación de Docker Desktop estaba cerrada o su motor interno seguía cargándose en segundo plano.
* **Solución:** Abre Docker Desktop desde el menú de inicio de Windows y espera pacientemente de 1 a 2 minutos hasta que el indicador visual en la esquina inferior izquierda pase a estar en **verde ("Engine running")**. Posteriormente, cierra tu ventana de terminal actual, abre una nueva e intenta ejecutar el comando otra vez.

**Alerta: "the attribute `version` is obsolete, it will be ignored"**
* **Por qué ocurre:** Las versiones más modernas de Docker Compose consideran redundante especificar la etiqueta `version: '3.9'` al inicio del archivo `.yml`.
* **Solución:** Es un Warning meramente informativo que no afecta en absoluto la compilación ni ejecución del proyecto. Puede ignorarse con total seguridad.

> El flag `-v` elimina el volumen de datos de PostgreSQL (`pgdata`), gestionado internamente por Docker. En el próximo `up`, PostgreSQL detecta que el volumen está vacío y **vuelve a ejecutar automáticamente** los scripts de `database/` desde cero. No es necesario borrar ninguna carpeta manualmente en ningún sistema operativo — Docker se encarga de todo.

**Para detener todos los contenedores sin borrar datos:**

```bash
docker-compose down
```

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

### 3.2 Tabla de endpoints principales

| Método | Endpoint                          | Roles autorizados            | Descripción                                                                        |
|--------|------------------------------------|-------------------------------|--------------------------------------------------------------------------------------|
| POST   | `/api/auth/login`                  | Público                       | Inicia sesión y retorna JWT.                                                          |
| GET    | `/api/students`                    | ADMIN                         | Lista todos los estudiantes.                                                          |
| GET    | `/api/students/{id}`               | ADMIN, PROFESSOR, STUDENT     | Obtiene datos de un estudiante.                                                       |
| GET    | `/api/students/{id}/curriculum`    | ADMIN, STUDENT                | Malla curricular con estado por asignatura (Aprobada, Reprobada, Cursando).           |
| POST   | `/api/students`                    | ADMIN                         | Crea un nuevo estudiante.                                                              |
| PUT    | `/api/students/{id}`               | ADMIN                         | Actualiza datos del estudiante.                                                        |
| DELETE | `/api/students/{id}`               | ADMIN                         | Elimina un estudiante.                                                                 |
| GET    | `/api/subjects`                    | Todos autenticados             | Lista todas las asignaturas.                                                            |
| POST   | `/api/subjects`                    | ADMIN                         | Crea una nueva asignatura.                                                              |
| PUT    | `/api/subjects/{id}`               | ADMIN                         | Actualiza una asignatura.                                                               |
| DELETE | `/api/subjects/{id}`               | ADMIN                         | Elimina una asignatura.                                                                 |
| GET    | `/api/sections`                    | Todos autenticados             | Lista todas las secciones.                                                              |
| POST   | `/api/sections`                    | ADMIN                         | Crea una nueva sección.                                                                 |
| GET    | `/api/semesters`                   | Todos autenticados             | Lista todos los semestres.                                                              |
| POST   | `/api/semesters`                   | ADMIN                         | Crea un nuevo semestre.                                                                 |
| PUT    | `/api/semesters/{id}`              | ADMIN                         | Actualiza un semestre.                                                                  |
| POST   | `/api/semesters/{id}/close`        | ADMIN                         | Cierra el semestre (stored procedure: calcula promedio ponderado, bloquea reprobados). |
| POST   | `/api/enrollments/enroll`          | STUDENT, ADMIN                | Inscribe al estudiante en una sección (stored procedure, valida prerrequisitos vía trigger). |
| DELETE | `/api/enrollments/{id}`            | STUDENT, ADMIN                | Cancela una inscripción y restaura el cupo de la sección.                              |
| GET    | `/api/enrollments/student/{id}`    | ADMIN, PROFESSOR, STUDENT     | Lista inscripciones del estudiante.                                                     |
| GET    | `/api/professors`                  | Todos autenticados             | Lista todos los profesores.                                                             |
| POST   | `/api/professors/grade`            | PROFESSOR                     | Ingresa una calificación (el trigger bloquea si está fuera del calendario).             |
| GET    | `/api/professors/reports`          | ADMIN, PROFESSOR              | Reporte de tasa de reprobación por asignatura (vista materializada).                    |

### 3.3 Ejemplos de solicitudes y respuestas

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

#### Subir una nota (PROFESSOR)
```
POST /api/professors/grade
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

#### Cancelar una inscripción (STUDENT)
```
DELETE /api/enrollments/6
Authorization: Bearer <token_estudiante>
```
> Elimina la inscripción y restaura el cupo en la sección correspondiente (`available_seats + 1`).

---

## 4. Objetos de base de datos implementados

### Triggers

| Nombre                       | Tabla        | Cuándo se activa     | Función                                                                |
|--------------------------------|--------------|-----------------------|---------------------------------------------------------------------------|
| `trg_check_prerequisites`    | `enrollment` | BEFORE INSERT         | Bloquea la inscripción si el estudiante no aprobó los prerrequisitos.    |
| `trg_check_calendario_notas` | `grade`      | BEFORE INSERT/UPDATE  | Impide ingresar notas fuera del calendario académico del semestre.       |

### Stored Procedures

| Nombre               | Descripción                                                                                  |
|------------------------|-------------------------------------------------------------------------------------------------|
| `sp_close_semester`  | Cierra el semestre: calcula promedio ponderado y bloquea estudiantes con promedio < 4.0.       |
| `sp_enroll_student`  | Inscribe a un estudiante en una sección en una transacción atómica, descontando un cupo.       |

### Vista materializada

| Nombre             | Descripción                                                                       |
|----------------------|----------------------------------------------------------------------------------|
| `mv_failure_rate`  | Tasa de reprobación histórica por asignatura.                                     |

### Índices

| Índice                     | Tabla / Columna                | Propósito                          |
|-------------------------------|-----------------------------------|----------------------------------------|
| `idx_usuario_rut`          | `usuario(rut)`                  | Búsqueda rápida por RUT.               |
| `idx_student_enrollment`   | `student(enrollment_number)`    | Búsqueda por número de matrícula.      |
| `idx_subject_code`         | `subject(code)`                 | Búsqueda por código de asignatura.     |

---

## 5. Estructura del repositorio

```
├── backend/                 # Proyecto Spring Boot (API REST)
│   ├── src/main/java/usach/cl/demo/
│   │   ├── config/          # JWT, Spring Security
│   │   ├── controller/      # Endpoints REST
│   │   ├── dto/             # Objetos de transferencia de datos
│   │   ├── model/           # Entidades
│   │   ├── repository/      # Acceso a BD con JdbcTemplate
│   │   └── service/         # Lógica de negocio
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                # Aplicación React
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   ├── router/
│   │   └── services/
│   ├── Dockerfile
│   └── package.json
├── database/                 # Scripts SQL — se cargan automáticamente al levantar Docker
│   ├── 1_db_schema.sql      # Tablas, relaciones, índices, triggers, stored procedures y vista materializada
│   └── 2_db_mock.sql        # Datos de prueba (usuarios, estudiantes, profesores, notas)
├── docker-compose.yml
├── .env                      # Configuración de puertos y credenciales (incluido en el repositorio)
└── README.md
```

> **Nota sobre `database/`:** los archivos deben mantener el prefijo numérico (`1_`, `2_`) y la extensión `.sql`. PostgreSQL ejecuta los scripts en `docker-entrypoint-initdb.d/` en orden alfabético, y solo reconoce archivos con extensión `.sh`, `.sql` o `.sql.gz`.

> **Nota sobre los datos de PostgreSQL:** el volumen `pgdata` es gestionado internamente por Docker (volumen con nombre, no una carpeta visible dentro del proyecto). No aparece como carpeta en el repositorio ni hay que administrarlo manualmente — se crea y destruye automáticamente con `docker-compose up` / `docker-compose down -v`.

### 5.1 Contenido de referencia — `docker-compose.yml`

```yaml
version: '3.9'
services:
  db:
    image: postgres:15
    container_name: postgres_db
    environment:
      - POSTGRES_USER=${POSTGRES_USER}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
      - POSTGRES_DB=${POSTGRES_DB}
    ports:
      - "${DB_HOST_PORT}:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./database:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 3s
      timeout: 6s
      retries: 5
      start_period: 10s
  backend:
    build:
      context: ./backend
    container_name: backend_tbd
    ports:
      - "${BACKEND_HOST_PORT}:${BACKEND_CONTAINER_PORT}"
    environment:
      - SERVER_PORT=${BACKEND_CONTAINER_PORT}
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/${POSTGRES_DB}
      - SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}
    depends_on:
      db:
        condition: service_healthy
  frontend:
    build:
      context: ./frontend
    container_name: frontend_tbd
    environment:
      - REACT_APP_API_URL=${REACT_APP_API_URL}
    ports:
      - "${FRONTEND_HOST_PORT}:3000"
    depends_on:
      - backend
volumes:
  pgdata:
```

> **Detalle importante:** en el servicio `db`, el volumen de datos se declara como `pgdata:/var/lib/postgresql/data` (**sin** `./` al inicio). Esto lo convierte en un *volumen con nombre* administrado por Docker, en vez de un *bind mount* a una carpeta local del proyecto. Con un bind mount (`./pgdata:/var/lib/postgresql/data`), `docker-compose down -v` no elimina los datos reales — solo borra un volumen sin uso, y los datos persisten silenciosamente en la carpeta local, causando comportamiento inconsistente entre reinicios.
