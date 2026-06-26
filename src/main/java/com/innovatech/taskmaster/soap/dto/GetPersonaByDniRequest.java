package com.innovatech.taskmaster.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "dni" })
@XmlRootElement(name = "GetPersonaByDniRequest", namespace = "http://innovatech.com/taskmaster/soap/dni")
public class GetPersonaByDniRequest {

    @XmlElement(namespace = "http://innovatech.com/taskmaster/soap/dni", required = true)
    private String dni;

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }
}
