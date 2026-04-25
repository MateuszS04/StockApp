package com.example.stockapp.controller;

import com.example.stockapp.dto.TradeRequest;
import com.example.stockapp.dto.TradeType;
import com.example.stockapp.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {
    private final TradeService tradeService;

    public WalletController(TradeService tradeService){
        this.tradeService=tradeService;
    }

    @PostMapping("/{walletId}/stocks/{stockName}")
    @ResponseStatus(HttpStatus.OK)
    public void trade(@PathVariable String walletId,
                      @PathVariable String stockName,
                      @Valid @RequestBody TradeRequest request){
        tradeService.trade(walletId, stockName, request.type());
    }
}
