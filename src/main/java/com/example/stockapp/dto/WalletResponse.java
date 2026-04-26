package com.example.stockapp.dto;

import java.util.List;

public record WalletResponse(String id, List<StockItem> stocks) {
}
