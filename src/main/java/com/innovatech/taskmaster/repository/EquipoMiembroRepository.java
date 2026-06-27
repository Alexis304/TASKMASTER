package com.innovatech.taskmaster.repository;

import com.innovatech.taskmaster.model.EquipoMiembro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoMiembroRepository extends JpaRepository<EquipoMiembro, Long> {

    List<EquipoMiembro> findByProyectoIdOrderByUsuario_NombresAsc(Long proyectoId);

    List<EquipoMiembro> findAllByOrderByProyecto_NombreAscUsuario_NombresAsc();

    boolean existsByProyectoIdAndUsuarioId(Long proyectoId, Long usuarioId);
}
