package com.innovatech.taskmaster.repository;

import com.innovatech.taskmaster.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaRepository extends JpaRepository<Tarea, Long> {
}
