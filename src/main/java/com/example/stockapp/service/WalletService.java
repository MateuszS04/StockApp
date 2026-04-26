package com.example.stockapp.service;

import com.example.stockapp.exception.StockNotKnownException;
import com.example.stockapp.dto.StockItem;
import com.example.stockapp.dto.WalletResponse;
import com.example.stockapp.exception.WalletNotKnownException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
public class WalletService {

    private final StringRedisTemplate redis;

    public WalletService(StringRedisTemplate redis){
        this.redis=redis;
    }

    public WalletResponse getWallet(String walletId){
        Boolean known= redis.opsForSet().isMember(TradeService.KNOWN_WALLET_SET, walletId);
        if(!Boolean.TRUE.equals(known)){
            throw new WalletNotKnownException(walletId);
        }
        Map<Object,Object> raw=redis.opsForHash().entries(TradeService.walletKey(walletId));

        List<StockItem> items = raw.entrySet().stream().map(e-> new StockItem(
                e.getKey().toString(), Integer.parseInt(e.getValue().toString()))).toList();

        return new WalletResponse(walletId, items);
    }

    public long getStockQuantity(String walletId, String stockName){
        Boolean stockKnown=redis.opsForSet().isMember(BankService.KNOWN_STOCKS_SET, stockName);
        if(!Boolean.TRUE.equals(stockKnown)){
            throw new StockNotKnownException(stockName);
        }

        Boolean walletKnown=redis.opsForSet().isMember(TradeService.KNOWN_WALLET_SET,walletId);
        if(!Boolean.TRUE.equals(walletKnown)){
            throw new WalletNotKnownException(walletId);
        }

        Object value= redis.opsForHash().get(TradeService.walletKey(walletId), stockName);
        return value ==null ? 0L : Long.parseLong(value.toString());
    }
}
