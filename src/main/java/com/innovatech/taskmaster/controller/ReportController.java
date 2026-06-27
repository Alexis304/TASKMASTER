package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.model.EstadoTarea;
import com.innovatech.taskmaster.service.BoardReportService;
import com.innovatech.taskmaster.soap.client.BoardReportPdf;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReportController {

    private final BoardReportService boardReportService;

    public ReportController(BoardReportService boardReportService) {
        this.boardReportService = boardReportService;
    }

    @GetMapping("/tablero.pdf")
    public ResponseEntity<byte[]> exportarTablero(
        @RequestParam(required = false) EstadoTarea estado,
        @RequestParam(required = false) Long proyectoId,
        @RequestParam(required = false) Long usuarioId,
        @RequestParam(required = false) String q,
        Authentication authentication
    ) {
        String generadoPor = authentication != null ? authentication.getName() : "Usuario TaskMaster";
        BoardReportPdf pdf = boardReportService.generarReporteTablero(estado, proyectoId, usuarioId, q, generadoPor);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(pdf.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.fileName()).build().toString())
            .body(pdf.content());
    }
}
