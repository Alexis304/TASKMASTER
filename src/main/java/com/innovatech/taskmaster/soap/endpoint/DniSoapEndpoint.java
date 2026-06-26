package com.innovatech.taskmaster.soap.endpoint;

import com.innovatech.taskmaster.soap.dto.GetPersonaByDniRequest;
import com.innovatech.taskmaster.soap.dto.GetPersonaByDniResponse;
import com.innovatech.taskmaster.soap.service.DniCatalogService;
import com.innovatech.taskmaster.soap.service.DniCatalogService.PersonaCatalogo;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class DniSoapEndpoint {

    private static final String NAMESPACE_URI = "http://innovatech.com/taskmaster/soap/dni";

    private final DniCatalogService dniCatalogService;

    public DniSoapEndpoint(DniCatalogService dniCatalogService) {
        this.dniCatalogService = dniCatalogService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetPersonaByDniRequest")
    @ResponsePayload
    public GetPersonaByDniResponse getPersonaByDni(@RequestPayload GetPersonaByDniRequest request) {
        PersonaCatalogo persona = dniCatalogService.buscarPorDni(request.getDni())
            .orElseThrow(() -> new DniSoapFaultException("DNI no encontrado"));

        GetPersonaByDniResponse response = new GetPersonaByDniResponse();
        response.setDni(persona.dni());
        response.setNombres(persona.nombres());
        response.setApellidoPaterno(persona.apellidoPaterno());
        response.setApellidoMaterno(persona.apellidoMaterno());
        response.setNombreCompleto(persona.nombreCompleto());
        response.setEstado(persona.estado());
        return response;
    }
}
