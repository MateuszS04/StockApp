package com.example.stockapp.exception;

public class StockNotKnownException extends RuntimeException{

    /**
     * Thrown when reauest reference a stock name that bank never had
     */

    public StockNotKnownException(String stockName){
        super("Unknown stock:"+stockName);
    }
}
