package com.example.stockapp.dto;

import jakarta.validation.constraints.NotNull;

public record TradeRequest(@NotNull TradeType type) {}
