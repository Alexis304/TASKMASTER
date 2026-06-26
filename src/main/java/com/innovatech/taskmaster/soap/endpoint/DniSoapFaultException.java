package com.innovatech.taskmaster.soap.endpoint;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class DniSoapFaultException extends RuntimeException {

    public DniSoapFaultException(String message) {
        super(message);
    }
}
