package model;

// Transaction: Data model class — mirrors the TRANSACTION table columns
// Created by RegexParser or BankSmsParser, then saved to DB by TransactionDAO

public class Transaction {

    // Fields match the TRANSACTION table columns exactly
    private int    userId;
    private int    merchantId;
    private int    categoryId;
    private int    sourceId;
    private double amount;
    private String transactionDate;  // ISO format: '2024-10-15'
    private String currency;         // 'INR', 'USD' etc.
    private String status;           // 'parsed', 'failed', 'credit'
    private String rawText;          // original unparsed SMS line

    // Constructor — called when creating a new Transaction object
    public Transaction(int userId, int merchantId, int categoryId,
                       int sourceId, double amount, String transactionDate,
                       String currency, String status, String rawText) {
        this.userId          = userId;
        this.merchantId      = merchantId;
        this.categoryId      = categoryId;
        this.sourceId        = sourceId;
        this.amount          = amount;
        this.transactionDate = transactionDate;
        this.currency        = currency;
        this.status          = status;
        this.rawText         = rawText;
    }

    // Getters — used by TransactionDAO when inserting into DB
    public int    getUserId()          { return userId; }
    public int    getMerchantId()      { return merchantId; }
    public int    getCategoryId()      { return categoryId; }
    public int    getSourceId()        { return sourceId; }
    public double getAmount()          { return amount; }
    public String getTransactionDate() { return transactionDate; }
    public String getCurrency()        { return currency; }
    public String getStatus()          { return status; }
    public String getRawText()         { return rawText; }

    @Override
    public String toString() {
        return String.format("[Transaction] Amount=%.2f | Date=%s | Currency=%s | Status=%s",
                amount, transactionDate, currency, status);
    }
}
