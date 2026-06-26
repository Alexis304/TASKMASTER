# TaskMaster API

TaskMaster API es un sistema de gestion de tareas corporativas orientado a centralizar la asignacion y seguimiento de proyectos mediante servicios web RESTful.

## Stack Tecnologico

- Java 17
- Spring Boot 3.x
- Maven
- PostgreSQL
- Spring Security
- Spring Web Services
- WebClient

## Objetivo Arquitectonico

El proyecto sigue una arquitectura por capas para separar responsabilidades:

```text
src/main/java/com/innovatech/taskmaster/
├── config/       # Configuraciones de seguridad, consumo externo y asincronia
├── controller/   # Endpoints REST
├── dto/          # Objetos de entrada y salida
├── model/        # Entidades JPA
├── repository/   # Acceso a datos
└── service/      # Logica de negocio e integraciones
```

## Funcionalidades Planeadas

### API REST

- `POST /api/tareas` para crear tareas.
- `GET /api/tareas` para listar tareas.
- `PUT /api/tareas/{id}` para actualizar el estado.
- `DELETE /api/tareas/{id}` para eliminar tareas.

### Seguridad

- Proteccion de endpoints con Spring Security.
- Cifrado de contrasenas con `BCryptPasswordEncoder`.
- Endpoint `/api/auth` para validar credenciales.

### Integracion SOAP

- Validacion de identidad al registrar usuarios usando un cliente SOAP.

### Integracion REST externa

- Validacion de fechas limite consultando feriados mediante API externa.

### Procesamiento asincrono

- Envio de notificaciones en segundo plano cuando se crea una tarea.

## Estructura Inicial del Dominio

- `Usuario`: responsable de autenticacion y asignacion.
- `Proyecto`: agrupa tareas de negocio.
- `Tarea`: unidad operativa del sistema con estado y fecha limite.

## Proximos pasos sugeridos

1. Completar persistencia y validaciones de negocio.
2. Implementar JWT o sesion segun el enfoque del curso.
3. Conectar el cliente SOAP real y la API publica de feriados.
4. Agregar pruebas unitarias e integracion con Postman.
