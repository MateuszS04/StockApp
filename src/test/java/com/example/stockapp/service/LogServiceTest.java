package com.example.stockapp.service;

import com.example.stockapp.dto.LogResponse;
import com.example.stockapp.model.AuditEntry;
import com.example.stockapp.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;


    @Test
    void getLog_returnsAllEntries_withTypeLowercased() {

        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                new AuditEntry("buy", "w1", "AAPL"),
                new AuditEntry("sell", "w2", "GOOG")
        ));


        LogService logService = new LogService(auditLogRepository);

        LogResponse response = logService.getLog();

        assertThat(response.log())
                .extracting("type", "walletId", "stockName")
                .containsExactly(
                        tuple("buy", "w1", "AAPL"),
                        tuple("sell", "w2", "GOOG")
                );
    }


    @Test
    void getLog_uppercaseTypeFromDb_isLowercasedInOutput() {

        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                new AuditEntry("BUY", "w1", "AAPL")
        ));

        LogService logService = new LogService(auditLogRepository);

        LogResponse response = logService.getLog();

        assertThat(response.log()).hasSize(1);

        assertThat(response.log().get(0).type()).isEqualTo("buy");
    }

    @Test
    void getLog_emptyDb_returnsEmptyList() {
        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        LogService logService = new LogService(auditLogRepository);

        LogResponse response = logService.getLog();

        assertThat(response.log()).isEmpty();
    }


}
