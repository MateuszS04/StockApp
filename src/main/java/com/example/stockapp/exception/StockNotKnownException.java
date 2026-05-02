package com.example.stockapp.exception;

public class StockNotKnownException extends RuntimeException{

    /**
     * Thrown when a request references a stock name that the bank never had.
     */

    // Builds the unknown-stock message from the name.
    public StockNotKnownException(String stockName){
        super("Unknown stock: " + stockName);
    }
}
