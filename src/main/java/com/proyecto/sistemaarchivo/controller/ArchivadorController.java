package com.proyecto.sistemaarchivo.controller;

import com.proyecto.sistemaarchivo.JWT.JwtUtils;
import com.proyecto.sistemaarchivo.model.Archivador;
import com.proyecto.sistemaarchivo.repository.ArchivadorRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // Import correcto de Spring
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/archivadores")
@CrossOrigin(origins = "*")
public class ArchivadorController {

    private static final Pattern ANIO_PATTERN = Pattern.compile("^(\\d{4})(?:\\s*-\\s*(\\d{4}))?$");

    @Autowired
    private ArchivadorRepository repository;

    @Autowired
    private JwtUtils jwtUtils;

    // 1. LISTAR TODOS
    @GetMapping
    public ResponseEntity<?> listar(HttpServletRequest request) {

        List<Map<String, Object>> resultados = repository.obtenerDetalleCompleto(null);

        if (esPrivilegiado(request)) {
            return ResponseEntity.ok(resultados);
        }

        Integer idDependencia = obtenerDependenciaToken(request);
        System.out.println("idDependencia  " + idDependencia);

        if (idDependencia == null) {
            System.out.println("entro linea 46  " + idDependencia);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
        }

        List<Map<String, Object>> filtrados = resultados.stream()
                .filter(r -> idDependencia.equals(mapToInt(r.get("idDependencia"))))
                .toList();
        return ResponseEntity.ok(filtrados);
    }

    // 2. BUSCAR CON FILTRO
    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) Integer idDependencia,
            @RequestParam(required = false) Integer idTipoArchivador,
            @RequestParam(required = false) String anio,
            @RequestParam(required = false) Integer esValioso,
            HttpServletRequest request) {

        System.out.println("ARCHIVADORES AUTH2: " + request.getHeader("Authorization"));
        System.out.println("ARCHIVADORES ROL2: " + obtenerRolToken(request));
        System.out.println("ARCHIVADORES ES PRIV2: " + esPrivilegiado(request));

        Integer idDependenciaFinal = idDependencia;
        if (!esPrivilegiado(request)) {
            Integer idDependenciaToken = obtenerDependenciaToken(request);
            System.out.println("idDependencia  " + idDependenciaToken);
            if (idDependenciaToken == null) {
                System.out.println("entro linea 75");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
            }
            if (idDependencia != null && !idDependencia.equals(idDependenciaToken)) {
                System.out.println("entro linea 76");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No puedes consultar archivadores de otra dependencia"));
            }
            idDependenciaFinal = idDependenciaToken;
        }

        String anioNormalizado = (anio != null && !anio.trim().isEmpty()) ? anio.trim() : null;

        List<Map<String, Object>> resultados = repository.filtrarArchivadoresPro(
                idDependenciaFinal, idTipoArchivador, anioNormalizado, esValioso
        );
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/por-documento")
    public ResponseEntity<?> buscarPorDocumento(@RequestParam String terminoDocumento) {
        return ejecutarBusquedaDocumento(terminoDocumento);
    }

    @GetMapping("/por-codigo-documento")
    public ResponseEntity<?> buscarPorCodigoDocumento(@RequestParam String codigoDocumento) {
        return ejecutarBusquedaDocumento(codigoDocumento);
    }

    private ResponseEntity<?> ejecutarBusquedaDocumento(String terminoDocumento) {
        try {
            if (terminoDocumento == null || terminoDocumento.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El parametro terminoDocumento es obligatorio"));
            }

            List<Map<String, Object>> resultados =
                    repository.buscarArchivadorPorTipoODocumento(terminoDocumento.trim());

            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al buscar por documento: " + e.getMessage()));
        }
    }

    // 3. OBTENER UNO POR ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id, HttpServletRequest request) {
        List<Map<String, Object>> detalle = repository.obtenerDetalleCompleto(id);
        if (detalle == null || detalle.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!esPrivilegiado(request)) {
            Integer idDependenciaToken = obtenerDependenciaToken(request);
            Integer idDependenciaRecurso = mapToInt(detalle.get(0).get("idDependencia"));
            if (idDependenciaToken == null || !idDependenciaToken.equals(idDependenciaRecurso)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para ver este archivador"));
            }
        }
        return ResponseEntity.ok(detalle.get(0));
    }

    // 4. CREAR ARCHIVADOR
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                Integer idDependenciaToken = obtenerDependenciaToken(request);
                if (idDependenciaToken == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No se pudo determinar la dependencia del usuario"));
                }

                Integer idDependenciaPayload = convertToInt(payload.get("idDependencia"));
                if (idDependenciaPayload != null && !idDependenciaPayload.equals(idDependenciaToken)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No puedes crear archivadores para otra dependencia"));
                }
                payload.put("idDependencia", idDependenciaToken);
            }

            Archivador arc = new Archivador();
            actualizarCampos(arc, payload);

            Archivador guardado = repository.save(arc);

            List<Map<String, Object>> detalle = repository.obtenerDetalleCompleto(guardado.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(!detalle.isEmpty() ? detalle.get(0) : guardado);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear: " + e.getMessage()));
        }
    }

    // 5. EDITAR ARCHIVADOR
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        return repository.findById(id).map(arc -> {
            try {
                if (!esPrivilegiado(request)) {
                    Integer idDependenciaToken = obtenerDependenciaToken(request);
                    if (idDependenciaToken == null || !idDependenciaToken.equals(arc.getIdDependencia())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "No tienes permiso para editar este archivador"));
                    }

                    Integer idDependenciaPayload = convertToInt(payload.get("idDependencia"));
                    if (idDependenciaPayload != null && !idDependenciaPayload.equals(idDependenciaToken)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "No puedes mover el archivador a otra dependencia"));
                    }
                    payload.put("idDependencia", idDependenciaToken);
                }

                actualizarCampos(arc, payload);
                repository.save(arc);

                List<Map<String, Object>> detalle = repository.obtenerDetalleCompleto(id);
                return ResponseEntity.ok(!detalle.isEmpty() ? detalle.get(0) : arc);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al editar: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // 6. ELIMINAR ARCHIVADOR
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Integer id, HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                Integer idDependenciaToken = obtenerDependenciaToken(request);
                Archivador arc = repository.findById(id).orElse(null);
                if (arc == null) {
                    return ResponseEntity.notFound().build();
                }
                if (idDependenciaToken == null || !idDependenciaToken.equals(arc.getIdDependencia())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No tienes permiso para eliminar este archivador"));
                }
            }

            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Archivador eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se puede eliminar: tiene registros vinculados."));
        }
    }

    // 7. VER DOCUMENTO DE UN ARCHIVADOR
    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> listarDocumentosPorArchivador(@PathVariable Integer id, HttpServletRequest request) {
        try {
            if (!esPrivilegiado(request)) {
                Archivador arc = repository.findById(id).orElse(null);
                Integer idDependenciaToken = obtenerDependenciaToken(request);
                if (arc == null) {
                    return ResponseEntity.notFound().build();
                }
                if (idDependenciaToken == null || !idDependenciaToken.equals(arc.getIdDependencia())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "No tienes permiso para ver documentos de este archivador"));
                }
            }

            // Asumiendo que tu repository tiene un método para esto
            // Si no lo tiene, deberás crearlo en DocumentoRepository
            List<Map<String, Object>> documentos = repository.obtenerDocumentosPorArchivador(id);

            if (documentos.isEmpty()) {
                return ResponseEntity.ok(List.of()); // Retorna lista vacía si no hay nada
            }

            return ResponseEntity.ok(documentos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener documentos: " + e.getMessage()));
        }
    }

    // LÓGICA DE MAPEADO
    private void actualizarCampos(Archivador arc, Map<String, Object> p) {
        if (p.containsKey("codigo")) {
            arc.setNumero(convertToInt(p.get("codigo")));
        } else if (p.containsKey("numero")) {
            arc.setNumero(convertToInt(p.get("numero")));
        }

        arc.setIdEstante(convertToInt(p.getOrDefault("idEstante", arc.getIdEstante())));
        arc.setIdDependencia(convertToInt(p.getOrDefault("idDependencia", arc.getIdDependencia())));
        arc.setIdTipoArchivador(convertToInt(p.getOrDefault("idTipoArchivador", arc.getIdTipoArchivador())));
        arc.setIdTipoDocumento(convertToInt(p.getOrDefault("idTipoDocumento", arc.getIdTipoDocumento())));
        arc.setIdCaja(convertToInt(p.getOrDefault("idCaja", arc.getIdCaja())));

        Object anioVal = p.get("año") != null ? p.get("año") : p.get("anio");
        if (anioVal != null) {
            String anioTexto = String.valueOf(anioVal).trim();
            if (!anioTexto.isEmpty()) {
                arc.setAño(validarYNormalizarAnio(anioTexto));
            }
        }

        arc.setCantidad_folio(convertToInt(p.getOrDefault("cantidad_folio", arc.getCantidad_folio())));
        arc.setCantidadDoc(convertToInt(p.getOrDefault("cantidadDoc", arc.getCantidadDoc())));
        arc.setDocumentoInicio(convertToInt(p.getOrDefault("documentoInicio", arc.getDocumentoInicio())));
        arc.setDocumentoFin(convertToInt(p.getOrDefault("documentoFin", arc.getDocumentoFin())));
        arc.setEs_valioso(convertToInt(p.getOrDefault("es_valioso", arc.getEs_valioso())));
        arc.setNumCuerpo(convertToInt(p.getOrDefault("numCuerpo", arc.getNumCuerpo())));
        arc.setValda(convertToValda(p.getOrDefault("valda", arc.getValda())));

        if (p.containsKey("unidadMedida")) {
            arc.setUnidadMedida(convertToDouble(p.get("unidadMedida")));
        }
    }

    // --- HELPERS ROBUSTOS ---
    private Integer convertToInt(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number n) return n.intValue();
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) { return null; }
    }

    private Double convertToDouble(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            if (obj instanceof Number n) return n.doubleValue();
            return Double.parseDouble(obj.toString().trim());
        } catch (Exception e) { return null; }
    }

    private String convertToValda(Object obj) {
        if (obj == null) return null;
        String val = String.valueOf(obj).trim().toUpperCase();
        return val.isEmpty() ? null : String.valueOf(val.charAt(0));
    }

    private String validarYNormalizarAnio(String valor) {
        Matcher matcher = ANIO_PATTERN.matcher(valor.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("El campo anio debe tener formato YYYY o YYYY-YYYY");
        }

        int inicio = Integer.parseInt(matcher.group(1));
        String finGrupo = matcher.group(2);
        int fin = (finGrupo != null) ? Integer.parseInt(finGrupo) : inicio;

        if (fin < inicio) {
            throw new IllegalArgumentException("El rango de anio es invalido: el anio final no puede ser menor al inicial");
        }

        int limite = Year.now().getValue() - 5;
        if (fin > limite) {
            throw new IllegalArgumentException("El anio final debe ser menor o igual a " + limite);
        }

        return (finGrupo == null) ? String.valueOf(inicio) : (inicio + "-" + fin);
    }

    private boolean esAdmin(HttpServletRequest request) {
        String rol = obtenerRolToken(request);
        return "ADMINISTRADOR".equalsIgnoreCase(rol);
    }

    private boolean esPrivilegiado(HttpServletRequest request) {
        String rol = obtenerRolToken(request);
        return "ADMINISTRADOR".equalsIgnoreCase(rol) || "USUARIOA".equalsIgnoreCase(rol);
    }

    private String obtenerRolToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            return jwtUtils.obtenerRolDelToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer obtenerDependenciaToken(HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null) return null;
        try {
            return jwtUtils.obtenerDependenciaDelToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }

    private Integer mapToInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}