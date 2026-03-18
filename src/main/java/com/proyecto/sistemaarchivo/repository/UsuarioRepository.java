package com.proyecto.sistemaarchivo.repository;

import com.proyecto.sistemaarchivo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // 1. Método fundamental para el Login y JWT
    Optional<Usuario> findByUsuario(String usuario);

    // 2. NUEVO: Para validar si el nombre de usuario ya existe al crear uno nuevo
    boolean existsByUsuario(String usuario);

    // 3. Consulta personalizada para el listado de administración
    @Query(value = "SELECT u.id, u.nombre, u.usuario, u.email, u.telefono, u.estado, u.bloqueado, u.IdRol, " +
            "r.nombre as nombre_rol, " +
            "d.nombre as nombre_dependencia, " +
            "s.nombre as nombre_sucursal " +
            "FROM usuario u " +
            "LEFT JOIN rol r ON u.IdRol = r.id " +
            "LEFT JOIN dependencia d ON u.IdDependencia = d.id " +
            "LEFT JOIN sucursal s ON u.IdSucursal = s.id",
            nativeQuery = true)
    List<Map<String, Object>> listarUsuariosConNombres();

    // Filtro de búsqueda avanzado
    @Query(value = "SELECT u.id, u.nombre, u.usuario, u.email, u.telefono, u.estado, u.bloqueado, u.IdRol, " +
            "r.nombre as nombre_rol, " +
            "d.nombre as nombre_dependencia, " +
            "s.nombre as nombre_sucursal " +
            "FROM usuario u " +
            "LEFT JOIN rol r ON u.IdRol = r.id " +
            "LEFT JOIN dependencia d ON u.IdDependencia = d.id " +
            "LEFT JOIN sucursal s ON u.IdSucursal = s.id " +
            "WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :criterio, '%')) " +
            "OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :criterio, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :criterio, '%')) " + // Campo email corregido
            "OR u.telefono LIKE %:criterio% " +
            "OR LOWER(r.nombre) LIKE LOWER(CONCAT('%', :criterio, '%')) " +
            "OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', :criterio, '%')) " +
            "OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :criterio, '%'))",
            nativeQuery = true)
    List<Map<String, Object>> buscarUsuariosPorCriterio(@Param("criterio") String criterio);
}
