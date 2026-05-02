package com.example.stockapp.service;


import com.example.stockapp.dto.LogEntry;
import com.example.stockapp.dto.LogResponse;
import com.example.stockapp.repository.AuditLogRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class LogService {
    private final AuditLogRepository auditLogRepository;

    // Stores the injected audit-log repository.
    public LogService(AuditLogRepository auditLogRepository){
        this.auditLogRepository=auditLogRepository;
    }

    // Returns all audit entries in insertion order.
    @Transactional(readOnly = true)
    public LogResponse getLog(){
        var entries = auditLogRepository.findAllByOrderByIdAsc().stream().map(
                e-> new LogEntry(
                        e.getType().toLowerCase(),
                        e.getWalletId(),
                        e.getStockName())).toList();
        return new LogResponse(entries);

    }

}
