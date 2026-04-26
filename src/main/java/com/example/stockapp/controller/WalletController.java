package com.example.stockapp.controller;

import com.example.stockapp.dto.TradeRequest;
import com.example.stockapp.dto.TradeType;
import com.example.stockapp.dto.WalletResponse;
import com.example.stockapp.service.TradeService;
import com.example.stockapp.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.batch.BatchTransactionManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {
    private final TradeService tradeService;
    private final WalletService walletService;

    public WalletController(TradeService tradeService, WalletService walletService){
        this.tradeService=tradeService;
        this.walletService=walletService;
    }

    @PostMapping("/{walletId}/stocks/{stockName}")
    @ResponseStatus(HttpStatus.OK)
    public void trade(@PathVariable String walletId,
                      @PathVariable String stockName,
                      @Valid @RequestBody TradeRequest request){
        tradeService.trade(walletId, stockName, request.type());
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(@PathVariable String walletId){
        return walletService.getWallet(walletId);
    }

    @GetMapping("/{walletId}/stocks/{stockName}")
    public long getStockQuantity(@PathVariable String walletId, @PathVariable String stockName){
        return walletService.getStockQuantity(walletId,stockName);
    }
}
