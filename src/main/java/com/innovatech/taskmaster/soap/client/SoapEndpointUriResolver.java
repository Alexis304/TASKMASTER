package com.innovatech.taskmaster.soap.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SoapEndpointUriResolver {

    private final Environment environment;
    private final String configuredEndpointUri;

    public SoapEndpointUriResolver(
        Environment environment,
        @Value("${taskmaster.soap.dni.endpoint-uri:}") String configuredEndpointUri
    ) {
        this.environment = environment;
        this.configuredEndpointUri = configuredEndpointUri;
    }

    public String resolveDniEndpointUri() {
        if (configuredEndpointUri != null && !configuredEndpointUri.isBlank()) {
            return configuredEndpointUri.trim();
        }

        String localServerPort = environment.getProperty("local.server.port");
        if (localServerPort != null && !localServerPort.isBlank()) {
            return "http://localhost:" + localServerPort + "/ws";
        }

        String serverPort = environment.getProperty("server.port", "8080");
        return "http://localhost:" + serverPort + "/ws";
    }
}
