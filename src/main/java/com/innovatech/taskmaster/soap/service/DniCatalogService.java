package com.innovatech.taskmaster.soap.service;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DniCatalogService {

    private final Map<String, PersonaCatalogo> personas = Map.of(
        "70112233", new PersonaCatalogo("70112233", "Alexis", "Ramirez", "Lopez", "Alexis Ramirez Lopez", "ACTIVO"),
        "70889911", new PersonaCatalogo("70889911", "Camila", "Torres", "Salazar", "Camila Torres Salazar", "ACTIVO"),
        "71234567", new PersonaCatalogo("71234567", "Mariana", "Paredes", "Diaz", "Mariana Paredes Diaz", "ACTIVO"),
        "74567890", new PersonaCatalogo("74567890", "Jorge", "Quispe", "Mendoza", "Jorge Quispe Mendoza", "ACTIVO"),
        "79999999", new PersonaCatalogo("79999999", "Lucia", "Prado", "Rios", "Lucia Prado Rios", "INACTIVO")
    );

    public Optional<PersonaCatalogo> buscarPorDni(String dni) {
        if (dni == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(personas.get(dni.trim()));
    }

    public record PersonaCatalogo(
        String dni,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String estado
    ) {
    }
}
