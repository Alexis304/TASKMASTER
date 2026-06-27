package com.innovatech.taskmaster.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.innovatech.taskmaster.soap.client.DniPersona;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class DniRestClient {

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiToken;

    public DniRestClient(
        WebClient webClient,
        @Value("${taskmaster.dni-api.url}") String apiUrl,
        @Value("${taskmaster.dni-api.token}") String apiToken
    ) {
        this.webClient = webClient;
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
    }

    public DniPersona obtenerPersonaPorDni(String dni) {
        if (!StringUtils.hasText(apiToken)) {
            throw new IllegalArgumentException("Configura TASKMASTER_DNI_API_TOKEN para validar DNI con la API REST.");
        }

        try {
            JsonNode response = webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("dni", dni))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> new IllegalArgumentException("No se encontro informacion para el DNI indicado.")))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.just(
                    new IllegalArgumentException("La API REST de DNI no esta disponible temporalmente.")
                ))
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(12));

            return toPersona(dni, response);
        } catch (WebClientResponseException exception) {
            throw new IllegalArgumentException("No se encontro informacion para el DNI indicado.");
        }
    }

    private DniPersona toPersona(String requestedDni, JsonNode response) {
        if (response == null || response.isMissingNode() || response.isNull()) {
            throw new IllegalArgumentException("La API REST de DNI no devolvio informacion valida.");
        }

        if (response.has("success") && !response.path("success").asBoolean()) {
            throw new IllegalArgumentException(readMessage(response));
        }

        JsonNode data = response.hasNonNull("data") ? response.path("data") : response;
        String dni = firstText(data, requestedDni, "numero", "dni", "documento", "numero_documento");
        String nombres = firstText(data, "", "nombres", "nombre");
        String apellidoPaterno = firstText(data, "", "apellido_paterno", "apellidoPaterno", "paterno");
        String apellidoMaterno = firstText(data, "", "apellido_materno", "apellidoMaterno", "materno");
        String nombreCompleto = firstText(data, "", "nombre_completo", "nombreCompleto", "razon_social");

        if (!StringUtils.hasText(nombreCompleto)) {
            nombreCompleto = String.join(" ", nombres, apellidoPaterno, apellidoMaterno).trim().replaceAll("\\s+", " ");
        }

        if (!StringUtils.hasText(nombreCompleto)) {
            throw new IllegalArgumentException("La API REST de DNI no devolvio nombres para el DNI indicado.");
        }

        String estado = firstText(data, "ACTIVO", "estado");
        return new DniPersona(dni, nombres, apellidoPaterno, apellidoMaterno, nombreCompleto, estado);
    }

    private String readMessage(JsonNode response) {
        String message = firstText(response, "", "message", "mensaje", "error");
        return StringUtils.hasText(message) ? message : "No se encontro informacion para el DNI indicado.";
    }

    private String firstText(JsonNode node, String defaultValue, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
            if (value.isNumber()) {
                return value.asText();
            }
        }
        return defaultValue;
    }
}
