# TaskMaster API

TaskMaster API es una aplicacion web funcional para gestionar tareas corporativas con Spring Boot. Incluye login por sesion, panel web estilo Kanban, API REST completa, servicio SOAP propio, validacion de DNI por SOAP, datos demo y una base preparada para integraciones externas y procesos asincronos.

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

- Login web con sesion segura y Google OAuth.
- Dashboard Kanban para listar, crear, mover, actualizar y eliminar tareas.
- API REST para usuarios, proyectos y tareas.
- Servicio SOAP propio para consulta de personas por DNI.
- Registro local validando DNI contra SOAP antes de guardar usuarios.
- Datos semilla para probar la app apenas arranque.
- Advertencia visual cuando la fecha limite cae en fin de semana.
- Servicio asincrono base para notificaciones.

## Credenciales demo

- Usuario: `admin@taskmaster.local`
- Password: `Admin123*`

## DNIs demo para registro SOAP

Puedes probar el registro local con estos DNIs:

- `70112233` -> Alexis Ramirez Lopez
- `70889911` -> Camila Torres Salazar
- `71234567` -> Mariana Paredes Diaz
- `74567890` -> Jorge Quispe Mendoza

El DNI `79999999` responde como `INACTIVO` y `70000000` genera error funcional.

## Login con Google

La interfaz ya deja preparado el acceso con Google. El boton se activa cuando configuras las credenciales OAuth del proveedor.

### 1. Crear el proyecto en Google Cloud

1. Entra a `https://console.cloud.google.com/`
2. Crea un proyecto nuevo o usa uno existente.
3. Ve a `APIs y servicios > Pantalla de consentimiento OAuth`.
4. Elige `Externo` si vas a probar con cuentas personales.
5. Completa nombre de la app, correo de soporte y dominios si Google te los pide.

### 2. Crear credenciales OAuth

1. Ve a `APIs y servicios > Credenciales`.
2. Haz clic en `Crear credenciales > ID de cliente de OAuth`.
3. Tipo de aplicacion: `Aplicacion web`.
4. Agrega este redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

Si arrancas la app en otro puerto, cambia el puerto aqui tambien. Por ejemplo, para `8081`:

```text
http://localhost:8081/login/oauth2/code/google
```

### 3. Configurar variables en tu entorno local

Antes de ejecutar la app, define estas variables en PowerShell:

```powershell
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID="pega-aqui-tu-client-id"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET="pega-aqui-tu-client-secret"
```

Luego inicia la aplicacion normalmente:

```powershell
mvn spring-boot:run
```

### 4. Usuarios de prueba

Si la app esta en modo `Externo` y aun no ha sido publicada por Google, agrega tu correo en:

`Pantalla de consentimiento OAuth > Usuarios de prueba`

### 5. Como validar que quedo bien

- El boton `Continuar con Google` se habilita en el login.
- Al hacer clic, Google muestra la pantalla de autorizacion.
- Al volver a la app, Spring redirige a `/login/oauth2/code/google`.
- Si todo salio bien, vuelves a la home con sesion iniciada.

## Ejecucion local

1. Instala Maven si aun no esta disponible en tu entorno.
2. Ejecuta `mvn spring-boot:run`.
3. Abre `http://localhost:8080`.

## Ejecucion con Docker

Si, el proyecto ya queda preparado para trabajar con Docker. La forma estable es usar `compose.yaml` con un archivo `.env`, para no estar corrigiendo puertos o credenciales cada vez.

1. Crea tu archivo `.env` tomando como base `.env.example`.
2. Si vas a usar Google Login, completa tambien `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`.
3. Si quieres forzar una URL especifica para el cliente SOAP, completa `TASKMASTER_SOAP_DNI_ENDPOINT_URI`. Si lo dejas vacio, la app se apunta sola a su endpoint local.
4. Levanta todo con:

```powershell
docker compose up --build
```

Luego abre `http://localhost:8080` o el puerto que definas en `APP_PORT`.

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
+-- soap/         # Endpoint, cliente, DTOs y configuracion SOAP
```

## Endpoints principales

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/usuarios`
- `POST /api/usuarios`
- `PUT /api/usuarios/{id}`
- `DELETE /api/usuarios/{id}`
- `GET /api/proyectos`
- `POST /api/proyectos`
- `PUT /api/proyectos/{id}`
- `DELETE /api/proyectos/{id}`
- `POST /api/tareas`
- `GET /api/tareas`
- `PUT /api/tareas/{id}`
- `DELETE /api/tareas/{id}`

Filtros REST para tareas:

- `GET /api/tareas?estado=PENDIENTE`
- `GET /api/tareas?proyectoId=1`
- `GET /api/tareas?usuarioId=2`
- `GET /api/tareas?q=kanban`

SOAP disponible:

- WSDL: `GET /ws/dni.wsdl`
- Endpoint SOAP: `/ws`
- Operacion: `GetPersonaByDni`

## Pendientes recomendados

1. Reemplazar la validacion local de fin de semana por la API real de feriados.
2. Conectar el cliente SOAP a una fuente externa real si el curso lo exige.
3. Agregar wrapper de Maven.
4. Evolucionar la seguridad a JWT si el curso lo requiere.
