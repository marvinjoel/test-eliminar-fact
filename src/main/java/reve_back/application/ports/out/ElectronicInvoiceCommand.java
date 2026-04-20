package reve_back.application.ports.out;

import reve_back.domain.model.Sale;

public record ElectronicInvoiceCommand(
        Sale sale,
        String companyRuc,
        String companyName,
        String companyAddress
) {
}
