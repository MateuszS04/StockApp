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

    // No-arg constructor required by JPA/Hibernate.
    protected AuditEntry(){
        // empty constructor for loading rows form database
    }

    // Builds an audit entry with the trade fields.
    public AuditEntry(String type, String walletId, String stockName){
        this.type=type;
        this.walletId=walletId;
        this.stockName=stockName;
    }

    // Stamps insertedAt before the row is persisted.
    @PrePersist
    void onPersist(){
        if(insertedAt==null){
            insertedAt=Instant.now();
        }
    }

    // Returns the database-generated primary key.
    public Long getId(){return id;}
    // Returns the trade type ("buy" or "sell").
    public String getType(){return type;}
    // Returns the wallet that performed the trade.
    public String getWalletId(){return walletId;}
    // Returns the stock that was traded.
    public String getStockName(){return stockName;}
    // Returns the timestamp the row was inserted.
    public Instant getInsertedAt(){return insertedAt;}
}
