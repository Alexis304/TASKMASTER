package com.innovatech.taskmaster.soap.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SoapEndpointUriResolver {

    private final Environment environment;
    private final String configuredDniEndpointUri;
    private final String configuredReportEndpointUri;

    public SoapEndpointUriResolver(
        Environment environment,
        @Value("${taskmaster.soap.dni.endpoint-uri:}") String configuredDniEndpointUri,
        @Value("${taskmaster.soap.report.endpoint-uri:}") String configuredReportEndpointUri
    ) {
        this.environment = environment;
        this.configuredDniEndpointUri = configuredDniEndpointUri;
        this.configuredReportEndpointUri = configuredReportEndpointUri;
    }

    public String resolveDniEndpointUri() {
        if (configuredDniEndpointUri != null && !configuredDniEndpointUri.isBlank()) {
            return configuredDniEndpointUri.trim();
        }

        return resolveLocalWsEndpoint();
    }

    public String resolveReportEndpointUri() {
        if (configuredReportEndpointUri != null && !configuredReportEndpointUri.isBlank()) {
            return configuredReportEndpointUri.trim();
        }

        return resolveLocalWsEndpoint();
    }

    private String resolveLocalWsEndpoint() {
        String localServerPort = environment.getProperty("local.server.port");
        if (localServerPort != null && !localServerPort.isBlank()) {
            return "http://localhost:" + localServerPort + "/ws";
        }

        String serverPort = environment.getProperty("server.port", "8080");
        return "http://localhost:" + serverPort + "/ws";
    }
}
