package com.example.stockapp.dto;

import java.util.Map;

public record StockHoldings(Map<String, Integer> stocks) {
}
