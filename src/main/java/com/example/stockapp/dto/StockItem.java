package com.example.stockapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record StockItem (
    @NotBlank String name,
    @NotNull @PositiveOrZero Integer quantity
){}
//record for immutable DTO and automatically makes getters, setters, constructor
//@positiveorZero for making sure that the quantity will stay always positive
// will be used for request (POST) payloads and response payloads (GET)