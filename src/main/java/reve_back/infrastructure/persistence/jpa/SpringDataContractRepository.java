package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.ContractEntity;

@RepositoryRestResource(exported = false)
public interface SpringDataContractRepository extends JpaRepository<ContractEntity, Long> {
    @Query("SELECT c FROM ContractEntity c ORDER BY c.createdAt DESC")
    Page<ContractEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM ContractEntity c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR LOWER(c.client.fullname) LIKE :search) " + // <-- CAMBIO AQUÍ
            "ORDER BY c.createdAt DESC")
    Page<ContractEntity> findContractsWithFilters(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}
