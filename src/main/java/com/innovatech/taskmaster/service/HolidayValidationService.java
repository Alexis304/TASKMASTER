package com.innovatech.taskmaster.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class HolidayValidationService {

    private final WebClient webClient;

    public HolidayValidationService(WebClient webClient) {
        this.webClient = webClient;
    }

    public boolean esFeriado(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }

        return false;
    }

    public WebClient getWebClient() {
        return webClient;
    }
}
