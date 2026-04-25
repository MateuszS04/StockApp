package com.example.stockapp.model;

import jakarta.persistence.*;
import java.time.Instant;
//No Lombok because it would automatically generate the setters for every field so we use
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length=4)
    private String type;

    @Column(name="wallet_id", nullable = false)
    private String walletId;

    @Column(name="stock_name", nullable = false)
    private String stockName;

    @Column(name="inserted_at", nullable = false, updatable = false)
    private Instant insertedAt;

    protected AuditEntry(){
        // empty constructor for loading rows form database
    }

    public AuditEntry(String type, String walletId, String stockName){
        this.type=type;
        this.walletId=walletId;
        this.stockName=stockName;
    }

    @PrePersist
    void onPersist(){
        if(insertedAt==null){
            insertedAt=Instant.now();
        }
    }

    public Long getId(){return id;}
    public String getType(){return type;}
    public String getWalletId(){return walletId;}
    public String getStockName(){return stockName;}
    public Instant getInsertedAt(){return insertedAt;}
}
