# TaskMaster API

TaskMaster API es una aplicacion web funcional para gestionar tareas corporativas con Spring Boot. Incluye login por sesion, panel web, CRUD de tareas, datos demo y una base preparada para integrar SOAP, REST externo y procesos asincronos.

## Stack

- Java 17
- Spring Boot 3.x
- Maven
- Spring Security
- Spring Data JPA
- H2 por defecto para demo local
- PostgreSQL mediante perfil `postgres`
- Spring Web Services
- WebClient

## Que incluye hoy

- Login web con sesion segura.
- Dashboard para listar, crear, actualizar y eliminar tareas.
- Catalogos de usuarios y proyectos para asignacion.
- Datos semilla para probar la app apenas arranque.
- Advertencia visual cuando la fecha limite cae en fin de semana.
- Servicio asincrono base para notificaciones.

## Credenciales demo

- Usuario: `admin@taskmaster.local`
- Password: `Admin123*`

## Ejecucion local

1. Instala Maven si aun no esta disponible en tu entorno.
2. Ejecuta `mvn spring-boot:run`.
3. Abre `http://localhost:8080`.

## Ejecucion con Docker

La forma mas practica para usar Docker en este proyecto es levantar la app y PostgreSQL juntos con Compose:

```powershell
docker compose up --build
```

Luego abre `http://localhost:8080`.

Servicios incluidos:

- `app`: Spring Boot en el puerto `8080`
- `postgres`: PostgreSQL en el puerto `5432`

Para detener todo:

```powershell
docker compose down
```

Si quieres borrar tambien la data persistida de PostgreSQL:

```powershell
docker compose down -v
```

## Base de datos

Por defecto la app usa H2 en archivo local para que arranque sin dependencias externas.

- Consola H2: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/taskmaster`
- User: `sa`
- Password: vacio

Si quieres usar PostgreSQL:

1. Crea la base `taskmaster`.
2. Ajusta credenciales en [application-postgres.properties](/C:/Users/Alexis/Documents/TASKMASTER/src/main/resources/application-postgres.properties).
3. Ejecuta `mvn spring-boot:run -Dspring-boot.run.profiles=postgres`.

Con Docker Compose no necesitas hacer esto manualmente, porque el contenedor `postgres` ya se crea con estas variables:

- Base: `taskmaster`
- Usuario: `taskmaster`
- Password: `taskmaster123`

## Arquitectura

```text
src/main/java/com/innovatech/taskmaster/
+-- config/       # Seguridad, asincronia y carga demo
+-- controller/   # Endpoints REST
+-- dto/          # Contratos de entrada y salida
+-- model/        # Entidades JPA
+-- repository/   # Acceso a datos
+-- service/      # Logica de negocio e integraciones
```

## Endpoints principales

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/catalogo/proyectos`
- `GET /api/catalogo/usuarios`
- `POST /api/tareas`
- `GET /api/tareas`
- `PUT /api/tareas/{id}?estado=COMPLETADA`
- `DELETE /api/tareas/{id}`

## Pendientes recomendados

1. Reemplazar la validacion local de fin de semana por la API real de feriados.
2. Implementar registro de usuarios con cliente SOAP real para validar DNI.
3. Agregar pruebas automatizadas y wrapper de Maven.
4. Evolucionar la seguridad a JWT si el curso lo requiere.
