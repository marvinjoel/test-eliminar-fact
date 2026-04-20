package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleCreationRequest(
        Long branchId,
        Long userId,
        Long clientId,
        BigDecimal systemDiscount,
        Long promotionId,
        String invoiceType, // "03" para Boleta, "01" para Factura, "00" para Ticket Interno

        List<SaleItemRequest> items,
        List<PaymentRequest> payments
) {
}
