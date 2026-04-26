package com.example.stockapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LogEntry(String type,
                       @JsonProperty("wallet_id") String walletId,
                       @JsonProperty("stock_name") String stockName) {
}
