package com.example.stockapp.service;

import com.example.stockapp.dto.SetStocksRequest;
import com.example.stockapp.dto.StockHoldings;
import com.example.stockapp.dto.StockItem;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankService {

    public static final String BANK_STOCKS_HASH="bank:stocks"; // holds quantities of stocks currently available
    public static final String KNOWN_STOCKS_SET="bank:known_stocks"; // tracks all stocks names that have ever existed
    // it is separate because when stock hits 0 it's still known
    // and the buy request won't return 404 but 400(sold out)
    private final StringRedisTemplate redis;

    public BankService(StringRedisTemplate redis){
        this.redis=redis;
    }

    public void setBankStocks(SetStocksRequest request){
        //iterating through the list
        Map<String, String> asString=new HashMap<>();
        for(StockItem item : request.stocks()){
            asString.put(item.name(), String.valueOf(item.quantity()));
        }
        // Reset both the quantities hash AND the known-stocks set so that
        // POST /stocks fully replaces bank state. Without clearing
        // KNOWN_STOCKS_SET, a stock omitted from the new payload would
        // still be considered "known" and a buy for it would return 400
        // (insufficient) instead of the spec-required 404 (unknown stock).
        redis.delete(BANK_STOCKS_HASH);
        redis.delete(KNOWN_STOCKS_SET);
        redis.opsForHash().putAll(BANK_STOCKS_HASH,asString);

        String[] names= request.stocks().stream().map(StockItem::name).toArray(String[]::new);
        redis.opsForSet().add(KNOWN_STOCKS_SET,names);
    }


    public StockHoldings getBankStocks(){
        //returns a list built from redis hash
        Map<Object, Object> raw=redis.opsForHash().entries(BANK_STOCKS_HASH);
        List<StockItem> items =raw.entrySet().stream()
                .map(e-> new StockItem(e.getKey().toString(),Integer.parseInt(e.getValue().toString()))).toList();

        return new StockHoldings(items);
    }
}
//redis storage format is unchanged still hold stock_name-> quantity
// I use the hash not JSON because I will use the LUA script which only
//works on hashes for atomic increments