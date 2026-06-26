package com.innovatech.taskmaster.repository;

import com.innovatech.taskmaster.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TareaRepository extends JpaRepository<Tarea, Long>, JpaSpecificationExecutor<Tarea> {

    boolean existsByUsuarioAsignadoId(Long usuarioId);

    boolean existsByProyectoId(Long proyectoId);
}
