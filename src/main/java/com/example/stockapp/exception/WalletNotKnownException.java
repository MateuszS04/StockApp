package com.example.stockapp.exception;

public class WalletNotKnownException extends RuntimeException {
    public WalletNotKnownException(String walletId) {
        super("Wallet not known"+ walletId);
    }
}
