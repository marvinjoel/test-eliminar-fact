package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record ProductSearchResponse(
        Long idInventario,
        String tipoVendible, // "BOTELLA" o "DECANT"
        Long productId,          // ID del producto raíz
        String brand,
        String line,
        String concentration,
        Integer volumeMl,        // Esencial para distinguir entre 10ml y 200ml
        BigDecimal price,        // Uso obligatorio de BigDecimal para moneda
        Boolean allowPromotions,
        Integer totalStockMl,
        String displayName       // Nombre formateado para la UI (ej. "ARIANA GRANDE CLOUD EDP (200ml)")
) {}
