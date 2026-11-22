package com.example.demo.dto;

/**
 * DTO de respuesta de reporte.
 */
public record ReporteResponse(
        Long id,
        String codigoTicket,
        Integer ambienteId,
        String ambienteNombre,
        Long docenteId,
        String docenteNombre,
        Long encargadoId,
        String encargadoNombre,
        String titulo,
        String descripcion,
        String fotoUrl,
        String prioridad,
        String estado,
        String observacionAtencion
) {
}
