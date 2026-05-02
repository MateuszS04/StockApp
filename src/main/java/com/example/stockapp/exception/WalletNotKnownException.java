package com.example.stockapp.exception;

public class WalletNotKnownException extends RuntimeException {
    // Builds the unknown-wallet message from the id.
    public WalletNotKnownException(String walletId) {
        super("Wallet not known: " + walletId);
    }
}
