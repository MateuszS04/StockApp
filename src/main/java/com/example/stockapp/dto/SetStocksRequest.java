package com.example.stockapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetStocksRequest (
        @NotEmpty List<@NotNull @Valid StockItem> stocks
        ) {}
