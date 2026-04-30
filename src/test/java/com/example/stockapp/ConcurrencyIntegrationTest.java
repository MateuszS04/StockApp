package com.example.stockapp;

import com.example.stockapp.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrencyIntegrationTest extends AbstractIntegrationTest{

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void resetState(){
        Set<String> keys=redis.keys("*");
        if(keys!=null && !keys.isEmpty()){
            redis.delete(keys);
        }
        auditLogRepository.deleteAll();
    }

    @Test
    void concurrentBuys_neverOversell_andAuditLogMatchesSuccesses() throws Exception {

        int initialQuantity =50;
        int totalAttempts = 100;

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> setupResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/stocks",
                new HttpEntity<>(
                        "{\"stocks\":[{\"name\":\"AAPL\",\"quantity\":" + initialQuantity + "}]}",
                        jsonHeaders),
                Void.class
        );

        assertThat(setupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        AtomicInteger successes= new AtomicInteger(0);
        AtomicInteger failures=new AtomicInteger(0);

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<?>> futures= new ArrayList<>();

        for (int i = 0; i < totalAttempts; i++) {

            final int walletId = i;

            futures.add(pool.submit(() -> {
                try {

                    startGate.await();

                    ResponseEntity<String> response = restTemplate.postForEntity(
                            "http://localhost:" + port + "/wallets/w" + walletId + "/stocks/AAPL",
                            new HttpEntity<>("{\"type\":\"BUY\"}", jsonHeaders),
                            String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK) {

                        successes.incrementAndGet();

                    } else if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {

                        failures.incrementAndGet();

                    } else {
                        throw new AssertionError("Unexpected status: " + response.getStatusCode());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        startGate.countDown();

        for (Future<?> future : futures) {
            future.get();
        }

        pool.shutdown();

        assertThat(successes.get()).isEqualTo(initialQuantity);
        assertThat(failures.get()).isEqualTo(totalAttempts - initialQuantity);

        String bankRemaining = (String) redis.opsForHash().get("bank:stocks", "AAPL");
        assertThat(bankRemaining).isEqualTo("0");

        long auditCount = auditLogRepository.count();
        assertThat(auditCount).isEqualTo(initialQuantity);
    }
}
