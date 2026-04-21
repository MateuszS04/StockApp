package com.example.stockapp.service;

import com.example.stockapp.dto.SetStocksRequest;
import com.example.stockapp.dto.StockHoldings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BankService {

    static final String BANK_STOCKS_HASH="bank:stocks"; // holds quantities of stocks currently available
    static final String KNOWN_STOCKS_SET="bank:known_stocks"; // tracks all stocks names that have ever existed
    // it is separate because when stock hits 0 it's still known
    // and the buy request won't return 404 but 400(sold out)
    private final StringRedisTemplate redis;

    public BankService(StringRedisTemplate redis){
        this.redis=redis;
    }

    public void setBankStocks(SetStocksRequest request){
        Map<String, String> asString=new HashMap<>();
        request.stocks().forEach((name,qty)-> asString.put(name,String.valueOf(qty)));
        redis.delete(BANK_STOCKS_HASH);
        redis.opsForHash().putAll(BANK_STOCKS_HASH,asString);
        redis.opsForSet().add(KNOWN_STOCKS_SET,request.stocks().keySet().toArray(new String[0]));

    }
    public StockHoldings getBankStocks(){
        Map<Object, Object> raw=redis.opsForHash().entries(BANK_STOCKS_HASH);
        Map<String, Integer> result=new HashMap<>();
        raw.forEach((k,v)-> result.put(k.toString(),Integer.parseInt(v.toString())));
        return new StockHoldings(result);
    }
}
