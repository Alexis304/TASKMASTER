package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.soap.client.DniPersona;
import com.innovatech.taskmaster.soap.client.SoapEndpointUriResolver;
import com.innovatech.taskmaster.soap.dto.GetPersonaByDniRequest;
import com.innovatech.taskmaster.soap.dto.GetPersonaByDniResponse;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Component
public class DniSoapClient {

    private final WebServiceTemplate webServiceTemplate;
    private final SoapEndpointUriResolver endpointUriResolver;

    public DniSoapClient(WebServiceTemplate webServiceTemplate, SoapEndpointUriResolver endpointUriResolver) {
        this.webServiceTemplate = webServiceTemplate;
        this.endpointUriResolver = endpointUriResolver;
    }

    public DniPersona obtenerPersonaPorDni(String dni) {
        try {
            GetPersonaByDniRequest request = new GetPersonaByDniRequest();
            request.setDni(dni);

            GetPersonaByDniResponse response = (GetPersonaByDniResponse) webServiceTemplate.marshalSendAndReceive(
                endpointUriResolver.resolveDniEndpointUri(),
                request
            );

            return new DniPersona(
                response.getDni(),
                response.getNombres(),
                response.getApellidoPaterno(),
                response.getApellidoMaterno(),
                response.getNombreCompleto(),
                response.getEstado()
            );
        } catch (SoapFaultClientException exception) {
            throw new IllegalArgumentException("No se encontro informacion para el DNI indicado.");
        }
    }
}
