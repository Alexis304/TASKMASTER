package com.innovatech.taskmaster.service;

import java.time.LocalDate;
import java.time.DayOfWeek;
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

        DayOfWeek dayOfWeek = fecha.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public String generarAdvertencia(LocalDate fecha) {
        return null;
    }

    public WebClient getWebClient() {
        return webClient;
    }
}
