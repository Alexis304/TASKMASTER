package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.soap.client.BoardReportPdf;
import com.innovatech.taskmaster.soap.client.SoapEndpointUriResolver;
import com.innovatech.taskmaster.soap.dto.GenerateBoardReportRequest;
import com.innovatech.taskmaster.soap.dto.GenerateBoardReportResponse;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

@Component
public class ReportSoapClient {

    private final WebServiceTemplate webServiceTemplate;
    private final SoapEndpointUriResolver endpointUriResolver;

    public ReportSoapClient(WebServiceTemplate webServiceTemplate, SoapEndpointUriResolver endpointUriResolver) {
        this.webServiceTemplate = webServiceTemplate;
        this.endpointUriResolver = endpointUriResolver;
    }

    public BoardReportPdf generarReporteTablero(
        String titulo,
        String generadoPor,
        String fechaGeneracion,
        String contenido
    ) {
        GenerateBoardReportRequest request = new GenerateBoardReportRequest();
        request.setTitulo(titulo);
        request.setGeneradoPor(generadoPor);
        request.setFechaGeneracion(fechaGeneracion);
        request.setContenido(contenido);

        GenerateBoardReportResponse response = (GenerateBoardReportResponse) webServiceTemplate.marshalSendAndReceive(
            endpointUriResolver.resolveReportEndpointUri(),
            request
        );

        return new BoardReportPdf(
            response.getFileName(),
            response.getContentType(),
            Base64.getDecoder().decode(response.getBase64Pdf())
        );
    }
}
