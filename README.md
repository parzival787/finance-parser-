# 💰 Finance Parser

A Java-based personal finance management application that automatically parses bank transaction SMS messages, extracts transaction details, categorizes expenses, stores financial data in MySQL, and generates financial reports and visual dashboards.

---

## 📌 Overview

**Finance Parser** is a desktop-based financial transaction processing system developed using Java. The application converts raw bank transaction SMS messages into structured financial records.

It extracts important information such as:

- Transaction amount
- Debit/Credit type
- Transaction date
- Bank name
- Merchant
- Account information
- UPI reference
- Transaction category

The processed information is stored in a **MySQL database** and can be analyzed through reports and graphical dashboards.

---

## ✨ Key Features

### 📩 Bank SMS Parsing

- Parses bank transaction SMS messages.
- Supports different bank SMS formats.
- Extracts transaction amount, date, bank, merchant, account information, and UPI reference.
- Includes dedicated parsing logic for Axis Bank and generic bank SMS formats.

### 🔍 Automatic Transaction Processing

The application converts raw SMS messages into structured transactions through the following pipeline:

```text
Raw Bank SMS
      ↓
SMS Format Detection
      ↓
Bank SMS Parser
      ↓
Transaction Information Extraction
      ↓
Duplicate Transaction Check
      ↓
Merchant Identification
      ↓
Automatic Categorization
      ↓
MySQL Database

🏗️ System Architecture
                         ┌──────────────────────┐
                         │      Bank SMS        │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    SMS File Reader   │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │  SMS Format Detection│
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
          ┌──────────────────┐            ┌──────────────────┐
          │ Axis SMS Parser  │            │ Generic Bank SMS │
          │                  │            │ Parser           │
          └────────┬─────────┘            └────────┬─────────┘
                   │                               │
                   └───────────────┬───────────────┘
                                   ▼
                         ┌──────────────────────┐
                         │ Duplicate Detection  │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │ Merchant Detection   │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │ Category Engine      │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    MySQL Database    │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
          ┌──────────────────┐            ┌──────────────────┐
          │ Financial Reports│            │ Chart Dashboard  │
          └──────────────────┘            └──────────────────┘

📂 Project Structure
          FinanceParser/
│
├── src/
│   ├── Main.java
│   │
│   ├── db/
│   │   ├── DBConnection.java
│   │   ├── SmsDAO.java
│   │   └── TransactionDAO.java
│   │
│   ├── gui/
│   │   └── FinanceGUI.java
│   │
│   ├── model/
│   │   ├── Category.java
│   │   ├── Merchant.java
│   │   └── Transaction.java
│   │
│   ├── parser/
│   │   ├── AxisSmsParser.java
│   │   ├── BankSmsParser.java
│   │   ├── CategoryEngine.java
│   │   ├── FileReader.java
│   │   ├── RegexParser.java
│   │   ├── SmsFileReader.java
│   │   └── SmsXmlConverter.java
│   │
│   └── report/
│       ├── ChartDataExporter.java
│       └── QueryRunner.java
│
├── resources/
│   └── sms_inbox
│
├── charts/
│   └── index.html
│
├── lib/
│   └── mysql-connector-j.jar
│
└── README.md
