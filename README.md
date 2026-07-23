# Sistema de Administración Académica Universitaria

Sistema de información multi-capa para la gestión académica universitaria. Permite administrar estudiantes, asignaturas, secciones, inscripciones, calificaciones, profesores y reportes, con autenticación basada en JWT y control de acceso por roles (ADMIN, PROFESSOR, STUDENT). Incorpora además un módulo geoespacial completo (Laboratorio 2): mapas del campus, ubicación en tiempo real, accesibilidad, optimización de traslados, geocodificación de direcciones y reportes con datos georreferenciados, mediante PostgreSQL + PostGIS. Desarrollado como parte de Taller de Base de Datos (USACH, 2026).

---

## 1. Arquitectura y tecnologías utilizadas

### Arquitectura general

- **Frontend**: Aplicación React de una sola página (SPA) que consume la API REST, con mapas interactivos renderizados mediante Leaflet.
- **Backend**: API REST desarrollada con Spring Boot, sin ORM. La lógica de negocio está delegada parcialmente a la base de datos mediante procedimientos almacenados, triggers y funciones espaciales de PostGIS.
- **Base de datos**: PostgreSQL con extensión PostGIS habilitada — triggers, stored procedures, vistas materializadas, índices B-tree e índices espaciales GIST.

### Tecnologías principales

| Capa          | Tecnologías / librerías                                                               |
| ------------- | ------------------------------------------------------------------------------------- |
| Backend       | Java 21, Spring Boot 4.0.6, Spring Security, Spring Data JDBC, JWT (jjwt), Lombok     |
| Documentación | springdoc-openapi (Swagger UI)                                                        |
| Base de datos | PostgreSQL 15 + PostGIS 3.4                                                           |
| Frontend      | React 18/19, React Router DOM, Axios, Bootstrap 5, React-Leaflet (mapas interactivos) |
| Contenedores  | Docker, Docker Compose                                                                |

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
  ```

> **Windows:** usar **PowerShell** (viene incluido en Windows) o la terminal de Git Bash (se instala junto con Git). No usar el CMD clásico, algunos comandos de este manual no son compatibles con él.

> **Linux:** si el sistema trae preinstalado el comando `docker-compose` (versión standalone 1.29.x, distinta del plugin moderno `docker compose`), y aparece un error del tipo `KeyError: 'ContainerConfig'` al reconstruir contenedores existentes, usar `docker compose` (con espacio) en su lugar — ver sección 2.3.

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
>
> ```bash
> docker compose up --build
> ```
>
> (sin guion, con espacio). Ambas formas son equivalentes — cambia solo según qué versión de Docker esté instalada. Esto aplica igual en Windows, Linux y Mac.

Este único comando:

1. Construye la imagen del backend (Spring Boot).
2. Construye la imagen del frontend (React).
3. Levanta PostgreSQL **con la extensión PostGIS habilitada** (imagen `postgis/postgis:15-3.4`).
4. **Crea automáticamente la base de datos y carga, en orden, los 4 scripts de `database/`**: tablas, triggers, stored procedures, vistas materializadas, índices (incluyendo los espaciales GIST) y datos de prueba — no requiere ninguna acción manual en pgAdmin ni en ningún otro cliente de base de datos.

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

| Verificación | URL                                         | Resultado esperado                                       |
| ------------ | ------------------------------------------- | -------------------------------------------------------- |
| Frontend     | http://localhost:3000                       | Pantalla de login del sistema                            |
| Backend      | http://localhost:9090                       | Respuesta del servidor (puede ser error JSON, es normal) |
| Swagger UI   | http://localhost:9090/swagger-ui/index.html | Documentación interactiva de la API                      |

Luego iniciar sesión con cada credencial para confirmar que los datos de prueba cargaron correctamente:

| Rol       | Email           | Contraseña | Qué debe verse              |
| --------- | --------------- | ---------- | --------------------------- |
| ADMIN     | admin@usach.cl  | 1234       | Dashboard de administración |
| STUDENT   | juan@usach.cl   | 1234       | Dashboard del estudiante    |
| PROFESSOR | carlos@usach.cl | 1234       | Vista del profesor          |

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

**Error: `No schema scripts found at location 'file:database/1_db_schema.sql'` (el backend no arranca)**

- **Por qué ocurre:** Spring Boot tiene su propio mecanismo de auto-inicialización de esquema (`spring.sql.init.*`), independiente y en conflicto con el mecanismo de PostgreSQL/Docker (`docker-entrypoint-initdb.d`) que este proyecto usa. Si `application.properties` llegara a incluir propiedades `spring.sql.init.mode`, `spring.sql.init.schema-locations`, etc., Spring Boot intentará leer los `.sql` desde una ruta relativa **dentro del contenedor del backend**, donde la carpeta `database/` no existe (esa carpeta solo se monta en el contenedor de PostgreSQL).
- **Solución:** Confirmar que `backend/src/main/resources/application.properties` **no contiene** ninguna línea `spring.sql.init.*`. La carga de scripts SQL para este proyecto es responsabilidad exclusiva del volumen de Docker (`docker-entrypoint-initdb.d`), definido en `docker-compose.yml` — no debe duplicarse desde el lado de Spring Boot.

**Error: "unable to get image... failed to connect to the docker API / El sistema no puede encontrar el archivo especificado"**

- **Por qué ocurre:** Intentaste ejecutar `docker compose` mientras la aplicación de Docker Desktop estaba cerrada o su motor interno seguía cargándose en segundo plano.
- **Solución:** Abre Docker Desktop desde el menú de inicio de Windows y espera pacientemente de 1 a 2 minutos hasta que el indicador visual en la esquina inferior izquierda pase a estar en **verde ("Engine running")**. Posteriormente, cierra tu ventana de terminal actual, abre una nueva e intenta ejecutar el comando otra vez.

**Alerta: "the attribute `version` is obsolete, it will be ignored"**

- **Por qué ocurre:** Las versiones más modernas de Docker Compose consideran redundante especificar la etiqueta `version: '3.9'` al inicio del archivo `.yml`.
- **Solución:** Es un Warning meramente informativo que no afecta en absoluto la compilación ni ejecución del proyecto. Puede ignorarse con total seguridad.

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

| Método                              | Endpoint                                | Roles autorizados             | Descripción                                                                                  |
| ----------------------------------- | --------------------------------------- | ----------------------------- | -------------------------------------------------------------------------------------------- |
| POST                                | `/api/auth/login`                       | Público                       | Inicia sesión y retorna JWT.                                                                 |
| **— Estudiantes —**                 |                                         |                               |                                                                                              |
| GET                                 | `/api/students`                         | ADMIN                         | Lista todos los estudiantes.                                                                 |
| GET                                 | `/api/students/{id}`                    | ADMIN, PROFESSOR, STUDENT     | Obtiene datos de un estudiante.                                                              |
| POST                                | `/api/students`                         | ADMIN                         | Crea un nuevo estudiante (también crea su usuario).                                          |
| PUT                                 | `/api/students/{id}`                    | ADMIN                         | Actualiza nombre, apellido y estado académico.                                               |
| DELETE                              | `/api/students/{id}`                    | ADMIN                         | Elimina un estudiante y su usuario vinculado.                                                |
| GET                                 | `/api/students/{id}/curriculum`         | ADMIN, STUDENT                | Malla curricular con estado por asignatura (PENDING, ENROLLED, APPROVED, FAILED).            |
| GET                                 | `/api/students/{id}/location`           | ADMIN, STUDENT                | Devuelve las coordenadas de vivienda guardadas del estudiante.                               |
| PATCH                               | `/api/students/{id}/location`           | ADMIN, STUDENT                | Actualiza la ubicación de vivienda del estudiante (lat/lng obtenidas por geocodificación).   |
| **— Asignaturas —**                 |                                         |                               |                                                                                              |
| GET                                 | `/api/subjects`                         | Todos autenticados            | Lista todas las asignaturas.                                                                 |
| POST                                | `/api/subjects`                         | ADMIN                         | Crea una nueva asignatura.                                                                   |
| PUT                                 | `/api/subjects/{id}`                    | ADMIN                         | Actualiza una asignatura.                                                                    |
| DELETE                              | `/api/subjects/{id}`                    | ADMIN                         | Elimina una asignatura.                                                                      |
| **— Secciones —**                   |                                         |                               |                                                                                              |
| GET                                 | `/api/sections`                         | Todos autenticados            | Lista todas las secciones con sala y horario.                                                |
| GET                                 | `/api/sections/{id}`                    | Todos autenticados            | Obtiene una sección por ID.                                                                  |
| POST                                | `/api/sections`                         | ADMIN                         | Crea una nueva sección. Valida conflicto de sala/horario y capacidad máxima.                 |
| PUT                                 | `/api/sections/{id}`                    | ADMIN                         | Edita una sección existente. Valida conflicto y capacidad.                                   |
| DELETE                              | `/api/sections/{id}`                    | ADMIN                         | Elimina una sección.                                                                         |
| GET                                 | `/api/sections/student/{id}`            | ADMIN, STUDENT                | Secciones activas en las que está inscrito el estudiante (para Mis Cursos y Mi Horario).     |
| GET                                 | `/api/sections/professor/{id}/active`   | ADMIN, PROFESSOR              | Secciones del semestre activo asignadas al profesor.                                         |
| GET                                 | `/api/sections/professor/{id}`          | ADMIN, PROFESSOR              | Todas las secciones históricas del profesor.                                                 |
| **— Semestres —**                   |                                         |                               |                                                                                              |
| GET                                 | `/api/semesters`                        | Todos autenticados            | Lista todos los semestres.                                                                   |
| POST                                | `/api/semesters`                        | ADMIN                         | Crea un nuevo semestre.                                                                      |
| PUT                                 | `/api/semesters/{id}`                   | ADMIN                         | Actualiza un semestre.                                                                       |
| POST                                | `/api/semesters/{id}/close`             | ADMIN                         | Cierra el semestre (stored procedure: calcula promedio ponderado, bloquea reprobados).       |
| **— Inscripciones —**               |                                         |                               |                                                                                              |
| GET                                 | `/api/enrollments/student/{id}`         | ADMIN, PROFESSOR, STUDENT     | Lista inscripciones de un estudiante.                                                        |
| GET                                 | `/api/enrollments/section/{id}`         | ADMIN, PROFESSOR              | Lista inscripciones de una sección.                                                          |
| POST                                | `/api/enrollments/enroll`               | ADMIN, STUDENT                | Inscribe al estudiante en una sección (stored procedure, valida prerrequisitos vía trigger). |
| PATCH                               | `/api/enrollments/{id}/status`          | ADMIN                         | Cambia el estado de una inscripción (ACTIVE, COMPLETED).                                     |
| DELETE                              | `/api/enrollments/{id}`                 | ADMIN, STUDENT                | Cancela una inscripción y restaura el cupo de la sección.                                    |
| **— Profesores —**                  |                                         |                               |                                                                                              |
| GET                                 | `/api/professors`                       | Todos autenticados            | Lista todos los profesores.                                                                  |
| GET                                 | `/api/professors/{id}`                  | Todos autenticados            | Obtiene un profesor por ID.                                                                  |
| POST                                | `/api/professors`                       | ADMIN                         | Crea un nuevo profesor (también crea su usuario con rol PROFESSOR).                          |
| PUT                                 | `/api/professors/{id}`                  | ADMIN                         | Actualiza nombre, departamento y opcionalmente email/contraseña.                             |
| DELETE                              | `/api/professors/{id}`                  | ADMIN                         | Elimina un profesor y su usuario vinculado.                                                  |
| GET                                 | `/api/professors/{id}/sections`         | ADMIN, PROFESSOR              | Todas las secciones asignadas al profesor.                                                   |
| POST                                | `/api/professors/grade`                 | PROFESSOR                     | Ingresa una calificación (el trigger bloquea si está fuera del calendario).                  |
| GET                                 | `/api/professors/reports`               | ADMIN, PROFESSOR              | Reporte de tasa de reprobación por asignatura (vista materializada).                         |
| **— Edificios y Salas —**           |                                         |                               |                                                                                              |
| GET/POST/PUT/DELETE                 | `/api/buildings`, `/api/buildings/{id}` | GET: todos · escritura: ADMIN | CRUD de edificios del campus (geometría `Polygon`).                                          |
| GET/POST/PUT/DELETE                 | `/api/rooms`, `/api/rooms/{id}`         | GET: todos · escritura: ADMIN | CRUD de salas (geometría `Point`). Soporta filtro `?buildingId=`.                            |
| GET                                 | `/api/rooms/accessible`                 | Todos autenticados            | Salas de un edificio marcadas como accesibles según cercanía a rampas (`?buildingId=`).      |
| **— Accesibilidad —**               |                                         |                               |                                                                                              |
| GET/POST/PUT/DELETE                 | `/api/accessibility-pois`               | GET: todos · escritura: ADMIN | CRUD de puntos de accesibilidad (rampas).                                                    |
| **— Ubicación (I2) —**              |                                         |                               |                                                                                              |
| POST                                | `/api/location/nearest-room`            | STUDENT, ADMIN                | Dada una ubicación GPS, retorna la sala más cercana con clase activa en ese momento.         |
| GET                                 | `/api/enrollments/nearby-sections`      | STUDENT, ADMIN                | Secciones de una asignatura ordenadas por distancia a una ubicación GPS.                     |
| **— Reportes geoespaciales (I4) —** |                                         |                               |                                                                                              |
| GET                                 | `/api/reports/density-heatmap`          | Todos autenticados            | Densidad estudiantil por edificio (vista materializada + GeoJSON).                           |
| GET                                 | `/api/reports/failure-by-district`      | Todos autenticados            | Tasa de reprobación por asignatura agrupada por distrito de vivienda.                        |
| POST                                | `/api/reports/refresh`                  | Todos autenticados            | Refresca manualmente las vistas materializadas geoespaciales.                                |

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

#### Actualizar ubicación de vivienda de un estudiante

```
PATCH /api/students/1/location
Authorization: Bearer <token_estudiante>
Content-Type: application/json
```

```json
{
  "latitude": -33.4489,
  "longitude": -70.6693
}
```

> Las coordenadas se obtienen en el frontend mediante geocodificación con Nominatim (OpenStreetMap) a partir de una dirección en texto ingresada por el estudiante. Se almacenan como `GEOMETRY(POINT, 4326)` en la columna `home_location` de la tabla `student`.

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

#### Crear una sección (validaciones automáticas)

```
POST /api/sections
Authorization: Bearer <token_admin>
Content-Type: application/json
```

```json
{
  "subjectId": 1,
  "professorId": 2,
  "semesterId": 2,
  "roomId": 1,
  "dayOfWeek": 3,
  "startTime": "08:15",
  "endTime": "09:35",
  "totalSeats": 30,
  "availableSeats": 30
}
```

> El backend valida dos restricciones antes de insertar:
>
> 1. **Conflicto de sala/horario:** si la sala ya está asignada a otra sección ese día en un bloque que se superpone, devuelve `400 Bad Request` con mensaje `"Conflicto de horario: la sala ya está ocupada en ese día y bloque"`.
> 2. **Capacidad de sala:** si `totalSeats` supera la capacidad física de la sala (`room.capacity`), devuelve `400 Bad Request`.

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

#### Sala más cercana con clase activa (STUDENT)

```
POST /api/location/nearest-room
Authorization: Bearer <token_estudiante>
Content-Type: application/json
```

```json
{
  "studentId": 1,
  "lat": -33.4488,
  "lng": -70.6845
}
```

Respuesta:

```json
{
  "roomId": 1,
  "roomCode": "A-101",
  "roomName": "Sala 101",
  "buildingId": 1,
  "sectionId": 9,
  "subjectId": 1,
  "subjectName": "Cálculo 1",
  "distanceMeters": 11.09,
  "geomGeoJson": "{\"type\":\"Point\",\"coordinates\":[-70.6845,-33.4487]}"
}
```

> Cruza la ubicación GPS enviada con las secciones cuyo horario (`day_of_week`, `start_time`, `end_time`) esté activo en el momento de la consulta, y calcula la distancia real con `ST_Distance` de PostGIS.

#### Salas accesibles de un edificio

```
GET /api/rooms/accessible?buildingId=1
Authorization: Bearer <token>
```

```json
[
  {
    "roomId": 1,
    "roomCode": "A-101",
    "accessible": true,
    "nearestRampMeters": 5.85
  },
  {
    "roomId": 2,
    "roomCode": "A-102",
    "accessible": true,
    "nearestRampMeters": 18.22
  },
  {
    "roomId": 3,
    "roomCode": "B-201",
    "accessible": false,
    "nearestRampMeters": 145.2
  }
]
```

> Marca una sala como accesible si existe un `accessibility_poi` (rampa) a menos de 50 metros, usando `ST_DWithin`.

### 3.4 Funcionalidades del frontend por rol

#### Panel Administrador (`/dashboard`)

| Sección         | Ruta          | Funcionalidades                                                                                                                                            |
| --------------- | ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Panel principal | `/dashboard`  | Resumen general del sistema                                                                                                                                |
| Carreras        | `/careers`    | CRUD completo                                                                                                                                              |
| Asignaturas     | `/subjects`   | CRUD completo                                                                                                                                              |
| Secciones       | `/sections`   | CRUD completo · columnas de sala y horario · validación de conflicto y capacidad al crear/editar                                                           |
| Semestres       | `/semesters`  | CRUD completo · cierre de semestre (stored procedure)                                                                                                      |
| Estudiantes     | `/students`   | CRUD completo · ver malla curricular · ver y gestionar inscripciones (mover entre secciones del semestre activo, cambiar estado, registrar nota, cancelar) |
| Profesores      | `/professors` | CRUD completo · ver cursos de cada profesor con filtro por semestre activo/todos                                                                           |
| Edificios       | `/buildings`  | CRUD con mapa interactivo (dibujo de polígono por clics)                                                                                                   |
| Salas           | `/rooms`      | CRUD con selección de punto en mapa · badge ♿ Accesible según cercanía a rampas                                                                           |
| Reportes        | `/reports`    | Tres pestañas: tasa de reprobación por asignatura · mapa de calor de densidad estudiantil · mapa de reprobación por distrito de vivienda                   |

#### Panel Estudiante

| Sección       | Ruta              | Funcionalidades                                                                                                                                                                                                             |
| ------------- | ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Inicio        | `/my-dashboard`   | Resumen con accesos directos                                                                                                                                                                                                |
| Mi Malla      | `/my-curriculum`  | Malla curricular completa con estado por asignatura (Pendiente, Cursando, Aprobado, Reprobado)                                                                                                                              |
| Mis Cursos    | `/my-courses`     | Secciones activas del semestre en curso con sala, día y horario                                                                                                                                                             |
| Mi Horario    | `/my-schedule`    | Horario semanal con bloques oficiales USACH (8:15–22:45, lunes a sábado)                                                                                                                                                    |
| Inscripciones | `/my-enrollments` | Ver inscripciones activas · cancelar · ir a inscribir nueva asignatura                                                                                                                                                      |
| Inscribir     | `/my-enroll`      | Inscripción en sección del semestre activo · filtra ramos ya aprobados e inscritos · muestra sala y horario antes de confirmar                                                                                              |
| Mis Notas     | `/my-grades`      | Notas del semestre actual                                                                                                                                                                                                   |
| Mi Perfil     | `/my-profile`     | Datos personales · sección de dirección: ingresar dirección en texto, geocodificación automática con Nominatim (OpenStreetMap), vista previa en mapa y confirmación antes de guardar · muestra la dirección guardada actual |
| Mi Ubicación  | `/my-location`    | Compartir ubicación GPS en tiempo real · buscar sala más cercana con clase activa                                                                                                                                           |

#### Panel Profesor

| Sección    | Ruta                  | Funcionalidades                                                                                                                                                              |
| ---------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Mi Panel   | `/professor`          | Estadísticas (secciones activas, total histórico, estudiantes inscritos) · tarjetas de secciones del semestre activo con sala y horario · acceso directo a notas por sección |
| Mis Cursos | `/professor/courses`  | Lista de secciones con sala y horario · filtro "solo semestre activo / todos los semestres" · acceso a gestión de notas por sección                                          |
| Mi Horario | `/professor/schedule` | Horario semanal con bloques oficiales USACH · detalle con número de estudiantes inscritos por sección                                                                        |
| Reportes   | `/reports`            | Mismas tres pestañas que el admin                                                                                                                                            |

### 3.5 Funcionalidades geoespaciales

| Funcionalidad                                           | Tecnología PostGIS                    | Rol              | Dónde          |
| ------------------------------------------------------- | ------------------------------------- | ---------------- | -------------- |
| Mapa de edificios (polígonos del campus)                | `GEOMETRY(POLYGON, 4326)`             | ADMIN            | `/buildings`   |
| Mapa de salas (puntos dentro de edificios)              | `GEOMETRY(POINT, 4326)`               | ADMIN            | `/rooms`       |
| Badge ♿ Accesible (`ST_DWithin` < 50m a rampa)         | `ST_DWithin`                          | Todos            | `/rooms`       |
| Ubicación en tiempo real + sala más cercana             | `ST_Distance`, `ST_SetSRID`           | STUDENT          | `/my-location` |
| Geocodificación de dirección (Nominatim → PostGIS)      | `ST_SetSRID(ST_MakePoint(...), 4326)` | STUDENT          | `/my-profile`  |
| Secciones cercanas al inscribirse (`ST_DWithin` < 300m) | `ST_DWithin`                          | STUDENT          | `/my-enroll`   |
| Mapa de calor de densidad estudiantil                   | Vista materializada + `ST_AsGeoJSON`  | ADMIN, PROFESSOR | `/reports`     |
| Mapa de reprobación por distrito de vivienda            | `ST_Contains` + vista materializada   | ADMIN, PROFESSOR | `/reports`     |

---

## 4. Objetos de base de datos implementados

### Tablas geoespaciales (Laboratorio 2)

| Tabla                   | Geometría             | Descripción                                                                 |
| ----------------------- | --------------------- | --------------------------------------------------------------------------- |
| `building`              | `Polygon` (SRID 4326) | Contorno de cada edificio del campus.                                       |
| `room`                  | `Point` (SRID 4326)   | Ubicación exacta de cada sala, asociada a un edificio.                      |
| `accessibility_poi`     | `Point` (SRID 4326)   | Puntos de interés de accesibilidad (rampas).                                |
| `housing_district`      | `Polygon` (SRID 4326) | Distritos de vivienda de la ciudad, usados para el reporte de zonificación. |
| `student.home_location` | `Point` (SRID 4326)   | Columna agregada a `student`: ubicación de vivienda del estudiante.         |

`section` fue extendida con `room_id`, `day_of_week`, `start_time` y `end_time` (todas `NOT NULL`), necesarias para determinar en qué sala y horario ocurre cada clase.

### Triggers

| Nombre                       | Tabla        | Cuándo               | Función                                                               |
| ---------------------------- | ------------ | -------------------- | --------------------------------------------------------------------- |
| `trg_check_prerequisites`    | `enrollment` | BEFORE INSERT        | Bloquea la inscripción si el estudiante no aprobó los prerrequisitos. |
| `trg_check_calendario_notas` | `grade`      | BEFORE INSERT/UPDATE | Impide ingresar notas fuera del calendario académico del semestre.    |

### Stored Procedures

| Nombre              | Descripción                                                                              |
| ------------------- | ---------------------------------------------------------------------------------------- |
| `sp_close_semester` | Cierra el semestre: calcula promedio ponderado y bloquea estudiantes con promedio < 4.0. |
| `sp_enroll_student` | Inscribe a un estudiante en una sección en una transacción atómica, descontando un cupo. |

### Vistas materializadas

| Nombre                           | Descripción                                                                                                                                        |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `mv_failure_rate`                | Tasa de reprobación histórica por asignatura.                                                                                                      |
| `mv_student_density_by_building` | Cantidad de estudiantes inscritos activos por edificio (mapa de calor de densidad).                                                                |
| `mv_failure_rate_by_district`    | Tasa de reprobación agrupada por asignatura y distrito de vivienda del estudiante. Usa `ST_Contains` para asociar cada estudiante con su distrito. |

> Las vistas materializadas geoespaciales se refrescan automáticamente al cargar el mock inicial, y pueden actualizarse manualmente vía `POST /api/reports/refresh`.

### Índices

| Índice                   | Tabla / Columna              | Tipo   | Propósito                                         |
| ------------------------ | ---------------------------- | ------ | ------------------------------------------------- |
| `idx_usuario_rut`        | `usuario(rut)`               | B-tree | Búsqueda rápida por RUT.                          |
| `idx_student_enrollment` | `student(enrollment_number)` | B-tree | Búsqueda por número de matrícula.                 |
| `idx_subject_code`       | `subject(code)`              | B-tree | Búsqueda por código de asignatura.                |
| `idx_building_geom`      | `building(geom)`             | GIST   | Consultas espaciales sobre edificios.             |
| `idx_room_geom`          | `room(geom)`                 | GIST   | Consultas de distancia y contención sobre salas.  |
| `idx_ramp_geom`          | `accessibility_poi(geom)`    | GIST   | Cálculo de cercanía a rampas.                     |
| `idx_district_geom`      | `housing_district(geom)`     | GIST   | Determinar en qué distrito vive un estudiante.    |
| `idx_student_home_geom`  | `student(home_location)`     | GIST   | Consultas espaciales sobre ubicación de vivienda. |

---

## 5. Estructura del repositorio

```
├── backend/                 # Proyecto Spring Boot (API REST)
│   ├── src/main/java/usach/cl/demo/
│   │   ├── config/          # JWT, Spring Security
│   │   ├── controller/      # Endpoints REST
│   │   ├── dto/             # Objetos de transferencia de datos
│   │   ├── model/           # Entidades
│   │   ├── repository/      # Acceso a BD con JdbcTemplate / JdbcClient
│   │   └── service/         # Lógica de negocio
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                # Aplicación React
│   ├── src/
│   │   ├── components/      # Navbar, MapView (Leaflet), AccessibilityBadge, PrivateRoute
│   │   ├── context/         # AuthContext (JWT decode, roles, login/logout)
│   │   ├── hooks/           # useGeolocation (Geolocation API del navegador)
│   │   ├── pages/
│   │   │   ├── buildings/   # BuildingList, BuildingForm (mapa interactivo)
│   │   │   ├── careers/     # CareerList, CareerForm
│   │   │   ├── professor/   # ProfessorDashboard, ProfessorCourses, ProfessorSchedule, ProfessorList, ProfessorForm, ProfessorCoursesAdmin
│   │   │   ├── reports/     # FailureReport, DensityHeatmap, DistrictFailureMap
│   │   │   ├── rooms/       # RoomList (con badge accesibilidad), RoomForm
│   │   │   ├── sections/    # SectionList, SectionForm (con validación de sala/horario)
│   │   │   ├── semesters/   # SemesterList, SemesterForm, SemesterClose
│   │   │   ├── students/    # StudentDashboard, StudentProfile (geocodificación), StudentCourses, StudentSchedule, StudentList, StudentForm, StudentGrades, StudentEnrollments, StudentEnrollmentsAdmin, EnrollForm, MyGrades, MyLocation
│   │   │   └── subjects/    # SubjectList, SubjectForm
│   │   ├── router/          # AppRouter (rutas por rol)
│   │   ├── services/        # api.js (axios + interceptor JWT)
│   │   └── utils/           # leafletIcons.js — fix de íconos de Leaflet en bundlers
│   ├── Dockerfile
│   └── package.json
├── database/                 # Scripts SQL — se cargan automáticamente al levantar Docker
│   ├── 1_db_schema.sql             # Tablas, relaciones, índices GIST, triggers, stored procedures, vistas materializadas
│   ├── 2_db_mock.sql               # Datos de prueba: usuarios, estudiantes, profesores, notas, edificios, salas, distritos, ubicaciones
│   ├── 3_i2_accessibility.sql      # Tabla, índice y datos de puntos de accesibilidad (rampas)
│   └── 4_i2_test_active_section.sql # Sección de prueba con ventana horaria dinámica para testear clase activa
├── docker-compose.yml
├── .env                      # Configuración de puertos y credenciales (incluido en el repositorio)
└── README.md
```

> **Nota sobre `database/`:** los archivos deben mantener el prefijo numérico (`1_`, `2_`, `3_`, `4_`...) y la extensión `.sql`. PostgreSQL ejecuta los scripts en `docker-entrypoint-initdb.d/` en orden alfabético, y solo reconoce archivos con extensión `.sh`, `.sql` o `.sql.gz`.

> **Nota sobre los datos de PostgreSQL:** el volumen `pgdata` es gestionado internamente por Docker (volumen con nombre, no una carpeta visible dentro del proyecto). No aparece como carpeta en el repositorio ni hay que administrarlo manualmente — se crea y destruye automáticamente con `docker-compose up` / `docker-compose down -v`.

### 5.1 Contenido de referencia — `docker-compose.yml`

```yaml
version: "3.9"
services:
  db:
    image: postgis/postgis:15-3.4
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

> **Detalle importante:** en el servicio `db`, el volumen de datos se declara como `pgdata:/var/lib/postgresql/data` (**sin** `./` al inicio). Esto lo convierte en un _volumen con nombre_ administrado por Docker, en vez de un _bind mount_ a una carpeta local del proyecto. Con un bind mount (`./pgdata:/var/lib/postgresql/data`), `docker-compose down -v` no elimina los datos reales — solo borra un volumen sin uso, y los datos persisten silenciosamente en la carpeta local, causando comportamiento inconsistente entre reinicios.

> **Por qué `postgis/postgis:15-3.4` y no `postgres:15`:** la imagen oficial de PostgreSQL no incluye la extensión PostGIS. La imagen `postgis/postgis` está mantenida por el mismo equipo de PostGIS, trae PostgreSQL 15 (idéntico al usado en Lab 1) más PostGIS 3.4 preinstalado, y es 100% compatible con el resto de la configuración (variables de entorno, puertos, volúmenes) sin ningún cambio adicional.

---

## 6. Integrantes y responsabilidades (Laboratorio 2)

| Integrante   | Módulo                                              | Descripción                                                                                                                                                                                                                                                                      |
| ------------ | --------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Integrante 1 | Sala y horario en sección                           | Extensión de la tabla `section` con `room_id`, `day_of_week`, `start_time`, `end_time`. Integración en formularios y listados del admin.                                                                                                                                         |
| Integrante 2 | Ubicación en tiempo real + Accesibilidad            | Endpoint `nearest-room`, tabla `accessibility_poi`, badge ♿ en salas, página `/my-location` del estudiante.                                                                                                                                                                     |
| Integrante 3 | Optimización de traslados (inscripción inteligente) |
| Integrante 4 | Vistas materializadas + Reportes geoespaciales      | Tabla `housing_district`, columna `home_location` en `student`, vistas materializadas `mv_student_density_by_building` y `mv_failure_rate_by_district`, endpoints `/api/reports/*`, componentes `DensityHeatmap`, `DistrictFailureMap` y sistema de pestañas en `FailureReport`. |
