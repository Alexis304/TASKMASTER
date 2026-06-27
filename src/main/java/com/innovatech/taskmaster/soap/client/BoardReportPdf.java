package com.innovatech.taskmaster.soap.client;

public record BoardReportPdf(
    String fileName,
    String contentType,
    byte[] content
) {
}
