package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.BillingSequenceEntity;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface SpringDataBillingSequenceRepository extends JpaRepository<BillingSequenceEntity, Long> {
    // Busca la secuencia exacta para una sede y tipo de comprobante
    Optional<BillingSequenceEntity> findByBranchIdAndInvoiceType(Long branchId, String invoiceType);
}
