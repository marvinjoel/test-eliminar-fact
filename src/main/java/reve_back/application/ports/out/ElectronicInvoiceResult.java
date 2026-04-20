package reve_back.application.ports.out;

public record ElectronicInvoiceResult(
        boolean success,
        String sunatStatus,    // "ACEPTADO", "RECHAZADO", "EXCEPCION"
        String message,        // Mensaje de SUNAT (ej. "La Factura F001-1 ha sido aceptada")
        String xmlUrl,         // Dónde guardamos el XML que generamos
        String cdrUrl          // Dónde guardamos el archivo de respuesta de SUNAT
) {
}
