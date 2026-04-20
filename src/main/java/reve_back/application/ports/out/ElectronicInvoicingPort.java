package reve_back.application.ports.out;

public interface ElectronicInvoicingPort {
    /**
     * Procesa una venta: Genera el XML, lo firma, lo envía a SUNAT y lee la respuesta.
     */
    ElectronicInvoiceResult sendInvoice(ElectronicInvoiceCommand request);
}
