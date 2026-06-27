package com.innovatech.taskmaster.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "titulo", "generadoPor", "fechaGeneracion", "contenido" })
@XmlRootElement(name = "GenerateBoardReportRequest", namespace = "http://innovatech.com/taskmaster/soap/reportes")
public class GenerateBoardReportRequest {

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/reportes", required = true)
    private String titulo;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/reportes", required = true)
    private String generadoPor;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/reportes", required = true)
    private String fechaGeneracion;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/reportes", required = true)
    private String contenido;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getGeneradoPor() {
        return generadoPor;
    }

    public void setGeneradoPor(String generadoPor) {
        this.generadoPor = generadoPor;
    }

    public String getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(String fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
