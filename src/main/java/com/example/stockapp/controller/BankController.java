package com.example.stockapp.controller;

import com.example.stockapp.dto.SetStocksRequest;
import com.example.stockapp.dto.StockHoldings;
import com.example.stockapp.service.BankService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService){
        this.bankService=bankService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void setStocks(@Valid @RequestBody SetStocksRequest request){
        bankService.setBankStocks(request);
    }

    @GetMapping
    public StockHoldings getStocks(){
        return bankService.getBankStocks();
    }
}
