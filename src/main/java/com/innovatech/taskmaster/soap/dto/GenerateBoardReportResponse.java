package com.innovatech.taskmaster.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "fileName", "contentType", "base64Pdf" })
@XmlRootElement(name = "GenerateBoardReportResponse", namespace = "http://innovatech.com/taskmaster/soap/reportes")
public class GenerateBoardReportResponse {

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/reportes", required = true)
    private String fileName;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/reportes", required = true)
    private String contentType;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/reportes", required = true)
    private String base64Pdf;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBase64Pdf() {
        return base64Pdf;
    }

    public void setBase64Pdf(String base64Pdf) {
        this.base64Pdf = base64Pdf;
    }
}
