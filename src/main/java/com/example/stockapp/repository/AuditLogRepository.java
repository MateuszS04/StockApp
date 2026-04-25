package com.example.stockapp.repository;

import com.example.stockapp.model.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditEntry, Long> {
    /**
     * Returns all audit entries in insertion order (id is bigserial, monotonically increasing)
     * used by GET /log
     */
    List<AuditEntry> findAllByOrderByIdAsc();
}
