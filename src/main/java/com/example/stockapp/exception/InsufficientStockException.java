package com.example.stockapp.exception;

public class InsufficientStockException extends RuntimeException {
    /**
     *Thrown when a buy/sell can't proceed because the source side has zero of the requested stock
     */

    public InsufficientStockException(String message ) {

        super(message);
    }
}
