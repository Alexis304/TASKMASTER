package com.innovatech.taskmaster.soap.endpoint;

import com.innovatech.taskmaster.soap.dto.GenerateBoardReportRequest;
import com.innovatech.taskmaster.soap.dto.GenerateBoardReportResponse;
import com.innovatech.taskmaster.soap.service.PdfReportGeneratorService;
import java.util.Base64;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class ReportesSoapEndpoint {

    private static final String NAMESPACE_URI = "http://innovatech.com/taskmaster/soap/reportes";

    private final PdfReportGeneratorService pdfReportGeneratorService;

    public ReportesSoapEndpoint(PdfReportGeneratorService pdfReportGeneratorService) {
        this.pdfReportGeneratorService = pdfReportGeneratorService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GenerateBoardReportRequest")
    @ResponsePayload
    public GenerateBoardReportResponse generateBoardReport(@RequestPayload GenerateBoardReportRequest request) {
        byte[] pdf = pdfReportGeneratorService.generarPdf(
            request.getTitulo(),
            request.getGeneradoPor(),
            request.getFechaGeneracion(),
            request.getContenido()
        );

        GenerateBoardReportResponse response = new GenerateBoardReportResponse();
        response.setFileName("taskmaster-tablero.pdf");
        response.setContentType("application/pdf");
        response.setBase64Pdf(Base64.getEncoder().encodeToString(pdf));
        return response;
    }
}
