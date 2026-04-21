package com.example.stockapp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public record SetStocksRequest (
    @NotEmpty
    Map<@NotNull String, @NotNull @PositiveOrZero Integer> stocks
) {}
