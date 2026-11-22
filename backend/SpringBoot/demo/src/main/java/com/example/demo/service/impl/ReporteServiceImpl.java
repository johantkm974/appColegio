package com.example.demo.service.impl;

import com.example.demo.dto.ReporteRequest;
import com.example.demo.dto.ReporteResponse;
import com.example.demo.model.Ambiente;
import com.example.demo.model.Reporte;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AmbienteRepository;
import com.example.demo.repository.HistorialReporteRepository;
import com.example.demo.repository.ReporteRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ReporteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReporteServiceImpl implements ReporteService {

    private final ReporteRepository reporteRepository;
    private final AmbienteRepository ambienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialReporteRepository historialReporteRepository;

    private static final String PREFIJO_TICKET = "REP";
    private static final String UPLOAD_DIR = "C:\\reporta_uploads\\";

    public ReporteServiceImpl(ReporteRepository reporteRepository,
                              AmbienteRepository ambienteRepository,
                              UsuarioRepository usuarioRepository,
                              HistorialReporteRepository historialReporteRepository) {
        this.reporteRepository = reporteRepository;
        this.ambienteRepository = ambienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialReporteRepository = historialReporteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listarTodos() {
        return reporteRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReporteResponse> buscarPorId(Long id) {
        return reporteRepository.findById(id).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReporteResponse> buscarPorCodigoTicket(String codigoTicket) {
        return reporteRepository.findByCodigoTicket(codigoTicket).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listarPorEstado(String estado) {
        return reporteRepository.findByEstado(estado).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listarPorDocente(Long docenteId) {
        return reporteRepository.findByDocente_Id(docenteId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listarPorEncargado(Long encargadoId) {
        return reporteRepository.findByEncargado_Id(encargadoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReporteResponse crear(ReporteRequest request, Long docenteId) {
        Ambiente ambiente = ambienteRepository.findById(request.ambienteId())
                .orElseThrow(() -> new IllegalArgumentException("Ambiente no encontrado"));

        Usuario docente = usuarioRepository.findById(docenteId)
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));

        String codigoTicket = generarCodigoTicket();

        Reporte reporte = new Reporte(
                codigoTicket,
                ambiente,
                docente,
                null, // encargado pendiente
                request.titulo(),
                request.descripcion(),
                request.fotoUrl(),
                request.prioridad() != null ? request.prioridad() : "MEDIA",
                request.estado() != null ? request.estado() : "POR_ATENDER"
        );
        reporte.setFechaReporte(LocalDateTime.now());
        reporte.setFechaActualizacion(LocalDateTime.now());

        Reporte saved = reporteRepository.save(reporte);

        // Registrar en historial
        registrarHistorial(saved, docente, null, saved.getEstado(), "Reporte creado");

        return toResponse(saved);
    }

    @Override
    public Optional<ReporteResponse> actualizarEstado(Long id, String nuevoEstado, String observacion, Long usuarioId) {
        return reporteRepository.findById(id).map(reporte -> {
            String estadoAnterior = reporte.getEstado();
            reporte.setEstado(nuevoEstado);
            reporte.setObservacionAtencion(observacion);
            reporte.setFechaActualizacion(LocalDateTime.now());
            if ("ATENDIDO".equalsIgnoreCase(nuevoEstado) || "RECHAZADO".equalsIgnoreCase(nuevoEstado)) {
                reporte.setFechaAtencion(LocalDateTime.now());
            }

            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            Reporte saved = reporteRepository.save(reporte);
            registrarHistorial(saved, usuario, estadoAnterior, nuevoEstado, observacion);

            return toResponse(saved);
        });
    }

    @Override
    public Optional<ReporteResponse> asignarEncargado(Long id, Long encargadoId) {
        return reporteRepository.findById(id).map(reporte -> {
            Usuario encargado = usuarioRepository.findById(encargadoId)
                    .orElseThrow(() -> new IllegalArgumentException("Encargado no encontrado"));
            reporte.setEncargado(encargado);
            reporte.setFechaActualizacion(LocalDateTime.now());
            return toResponse(reporteRepository.save(reporte));
        });
    }

    @Override
    public void eliminar(Long id) {
        reporteRepository.deleteById(id);
    }

    private String generarCodigoTicket() {
        long count = reporteRepository.count();
        int numero = (int) (count + 1);
        return String.format("%s-%d-%04d", PREFIJO_TICKET, java.time.Year.now().getValue(), numero);
    }

    private void registrarHistorial(Reporte reporte, Usuario usuario, String estadoAnterior,
                                    String estadoNuevo, String comentario) {
        var historial = new com.example.demo.model.HistorialReporte(
                reporte, usuario, estadoAnterior, estadoNuevo, comentario
        );
        historialReporteRepository.save(historial);
    }

    private ReporteResponse toResponse(Reporte r) {
        return new ReporteResponse(
                r.getId(),
                r.getCodigoTicket(),
                r.getAmbiente() != null ? r.getAmbiente().getId() : null,
                r.getAmbiente() != null ? r.getAmbiente().getNombre() : null,
                r.getDocente() != null ? r.getDocente().getId() : null,
                r.getDocente() != null ? r.getDocente().getNombreCompleto() : null,
                r.getEncargado() != null ? r.getEncargado().getId() : null,
                r.getEncargado() != null ? r.getEncargado().getNombreCompleto() : null,
                r.getTitulo(),
                r.getDescripcion(),
                r.getFotoUrl(),
                r.getPrioridad(),
                r.getEstado(),
                r.getObservacionAtencion()
        );
    }

    @Override
    public ReporteResponse subirFoto(Long id, MultipartFile foto) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado con id: " + id));

        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Nombre único del archivo
            String nombreArchivo = "reporte_" + id + "_" + System.currentTimeMillis() + "_" + foto.getOriginalFilename();
            Path archivoPath = uploadPath.resolve(nombreArchivo);
            Files.copy(foto.getInputStream(), archivoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Guardar URL relativa o absoluta
            String fotoUrl = archivoPath.toString();
            reporte.setFotoUrl(fotoUrl);
            reporte.setFechaActualizacion(LocalDateTime.now());

            Reporte saved = reporteRepository.save(reporte);
            return toResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir la imagen: " + e.getMessage());
        }
    }
}
