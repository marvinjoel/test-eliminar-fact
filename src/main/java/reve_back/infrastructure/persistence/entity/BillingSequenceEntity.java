package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "billing_sequences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"branch_id", "invoice_type", "series"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingSequenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(name = "invoice_type", nullable = false, length = 2)
    private String invoiceType; // "03" (Boleta), "01" (Factura)

    @Column(nullable = false, length = 4)
    private String series; // "B001", "F001"

    @Column(nullable = false)
    private Integer currentCorrelative; // El último número usado (Ej: 15)
}
