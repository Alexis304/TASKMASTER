package com.innovatech.taskmaster.soap.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.springframework.ws.client.core.WebServiceTemplate;

@EnableWs
@Configuration
public class SoapWebServiceConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "dni")
    public DefaultWsdl11Definition dniWsdlDefinition(XsdSchema dniSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("DniPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace("http://innovatech.com/taskmaster/soap/dni");
        definition.setSchema(dniSchema);
        return definition;
    }

    @Bean
    public XsdSchema dniSchema() {
        return new SimpleXsdSchema(new ClassPathResource("soap/dni.xsd"));
    }

    @Bean
    public Jaxb2Marshaller soapMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.innovatech.taskmaster.soap.dto");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller soapMarshaller) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(soapMarshaller);
        template.setUnmarshaller(soapMarshaller);
        return template;
    }
}
