package com.innovatech.taskmaster.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dni",
    "nombres",
    "apellidoPaterno",
    "apellidoMaterno",
    "nombreCompleto",
    "estado"
})
@XmlRootElement(name = "GetPersonaByDniResponse", namespace = "http://innovatech.com/taskmaster/soap/dni")
public class GetPersonaByDniResponse {

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/dni", required = true)
    private String dni;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/dni", required = true)
    private String nombres;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/dni", required = true)
    private String apellidoPaterno;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/dni", required = true)
    private String apellidoMaterno;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/dni", required = true)
    private String nombreCompleto;

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/dni", required = true)
    private String estado;

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
