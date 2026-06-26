package com.innovatech.taskmaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovatech.taskmaster.dto.AuthRequest;
import com.innovatech.taskmaster.dto.CurrentUserResponse;
import com.innovatech.taskmaster.dto.ProyectoResponse;
import com.innovatech.taskmaster.dto.RegisterRequest;
import com.innovatech.taskmaster.dto.TareaCreateRequest;
import com.innovatech.taskmaster.dto.TareaResponse;
import com.innovatech.taskmaster.dto.TareaUpdateRequest;
import com.innovatech.taskmaster.dto.UsuarioCreateRequest;
import com.innovatech.taskmaster.dto.UsuarioResponse;
import com.innovatech.taskmaster.model.EstadoTarea;
import com.innovatech.taskmaster.service.DniSoapClient;
import com.innovatech.taskmaster.soap.dto.GetPersonaByDniRequest;
import com.innovatech.taskmaster.soap.dto.GetPersonaByDniResponse;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ws.client.core.WebServiceTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:taskmastertest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "taskmaster.soap.dni.endpoint-uri="
})
class TaskmasterApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    @Autowired
    private DniSoapClient dniSoapClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUserUsingSoapValidationAndKeepSession() throws Exception {
        SessionHttpClient client = sessionClient();
        String email = "soap-user-" + System.nanoTime() + "@taskmaster.local";

        HttpResponse<String> registerResponse = client.post("/api/auth/register", new RegisterRequest("71234567", email, "Password123*"));
        assertEquals(200, registerResponse.statusCode());

        Map<String, Object> authPayload = readMap(registerResponse.body());
        assertEquals(email, authPayload.get("email"));
        assertEquals("Mariana Paredes Diaz", authPayload.get("nombres"));

        HttpResponse<String> meResponse = client.get("/api/auth/me");
        assertEquals(200, meResponse.statusCode());

        CurrentUserResponse currentUser = objectMapper.readValue(meResponse.body(), CurrentUserResponse.class);
        assertEquals(email, currentUser.email());
        assertEquals("Mariana Paredes Diaz", currentUser.nombres());
    }

    @Test
    void shouldRejectRegisterWhenDniDoesNotExist() throws Exception {
        SessionHttpClient client = sessionClient();
        String email = "missing-dni-" + System.nanoTime() + "@taskmaster.local";

        HttpResponse<String> response = client.post("/api/auth/register", new RegisterRequest("70000000", email, "Password123*"));
        assertEquals(400, response.statusCode());

        Map<String, Object> errorPayload = readMap(response.body());
        assertTrue(String.valueOf(errorPayload.get("message")).contains("DNI"));
    }

    @Test
    void shouldExposeSoapEndpointAndResolveClientFaults() {
        GetPersonaByDniRequest request = new GetPersonaByDniRequest();
        request.setDni("70112233");

        GetPersonaByDniResponse response = (GetPersonaByDniResponse) webServiceTemplate.marshalSendAndReceive(
            url("/ws"),
            request
        );

        assertNotNull(response);
        assertEquals("70112233", response.getDni());
        assertEquals("Alexis Ramirez Lopez", response.getNombreCompleto());
        assertEquals("ACTIVO", response.getEstado());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dniSoapClient.obtenerPersonaPorDni("70000000")
        );
        assertTrue(exception.getMessage().contains("DNI"));
    }

    @Test
    void shouldSupportCrudAndFiltersForMainRestApi() throws Exception {
        SessionHttpClient client = sessionClient();
        loginAsAdmin(client);

        HttpResponse<String> projectResponse = client.post(
            "/api/proyectos",
            Map.of("nombre", "Proyecto QA", "descripcion", "Pruebas de integracion")
        );
        assertEquals(200, projectResponse.statusCode());
        ProyectoResponse project = objectMapper.readValue(projectResponse.body(), ProyectoResponse.class);

        String userEmail = "board-user-" + System.nanoTime() + "@taskmaster.local";
        HttpResponse<String> userResponse = client.post(
            "/api/usuarios",
            new UsuarioCreateRequest("74567890", userEmail, "Password123*")
        );
        assertEquals(200, userResponse.statusCode());
        UsuarioResponse user = objectMapper.readValue(userResponse.body(), UsuarioResponse.class);

        HttpResponse<String> taskResponse = client.post(
            "/api/tareas",
            new TareaCreateRequest(
                "Preparar pruebas SOAP",
                "Cubrir flujo completo",
                LocalDate.now().plusDays(3),
                project.id(),
                user.id()
            )
        );
        assertEquals(200, taskResponse.statusCode());
        TareaResponse task = objectMapper.readValue(taskResponse.body(), TareaResponse.class);

        HttpResponse<String> filteredResponse = client.get(
            "/api/tareas?estado=PENDIENTE&proyectoId=" + project.id() + "&usuarioId=" + user.id() + "&q=SOAP"
        );
        assertEquals(200, filteredResponse.statusCode());
        TareaResponse[] filteredTasks = objectMapper.readValue(filteredResponse.body(), TareaResponse[].class);
        assertEquals(1, filteredTasks.length);
        assertEquals(task.id(), filteredTasks[0].id());

        HttpResponse<String> updatedResponse = client.put(
            "/api/tareas/" + task.id(),
            new TareaUpdateRequest(
                "Preparar pruebas REST y SOAP",
                "Cobertura actualizada",
                LocalDate.now().plusDays(5),
                EstadoTarea.EN_PROGRESO,
                project.id(),
                user.id()
            )
        );
        assertEquals(200, updatedResponse.statusCode());
        TareaResponse updatedTask = objectMapper.readValue(updatedResponse.body(), TareaResponse.class);
        assertEquals(EstadoTarea.EN_PROGRESO, updatedTask.estado());

        HttpResponse<String> deleteResponse = client.delete("/api/tareas/" + task.id());
        assertEquals(204, deleteResponse.statusCode());
    }

    private void loginAsAdmin(SessionHttpClient client) throws Exception {
        HttpResponse<String> response = client.post("/api/auth/login", new AuthRequest("admin@taskmaster.local", "Admin123*"));
        assertEquals(200, response.statusCode());
    }

    private SessionHttpClient sessionClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .build();
        return new SessionHttpClient(httpClient);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Map<String, Object> readMap(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() { });
    }

    private class SessionHttpClient {

        private final HttpClient httpClient;

        private SessionHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        private HttpResponse<String> get(String path) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Accept", "application/json")
                .GET()
                .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> post(String path, Object payload) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(payload)))
                .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> put(String path, Object payload) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(payload)))
                .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> delete(String path) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Accept", "application/json")
                .DELETE()
                .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
