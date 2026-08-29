<div align="center">

# 💳 FinanceParser

**Automatically parse Indian bank SMS alerts → MySQL → interactive spending dashboard**

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?style=flat-square&logo=mysql)
![JDBC](https://img.shields.io/badge/JDBC-mysql--connector--j-lightgrey?style=flat-square)
![Chart.js](https://img.shields.io/badge/Chart.js-4.4.0-pink?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)

</div>

---

Most Indian banks send an SMS for every transaction. FinanceParser reads those messages from a text file, extracts amount, date, merchant, and transaction type using regex, persists everything to MySQL, and produces a browser-ready dashboard — **no manual data entry required.**

---

## ✨ Features

- 🏦 Supports **Axis Bank** (multi-line) and **SBI, HDFC, ICICI, Kotak, PNB, BOB, Canara, Paytm, PhonePe** (single-line)
- 🔁 **Idempotent** — duplicate UPI refs are detected and skipped on re-runs
- 🏷️ **Auto-categorization** via configurable keyword rules stored in the DB (no code change needed)
- 📊 **HTML dashboard** with pie, bar, and line charts (Chart.js)
- 🖨️ **Console reports** — spending by category, monthly trend, budget vs actual, top merchants
- 📦 **Android XML import** — converts SMS Backup & Restore exports directly to the input format

---

## 📸 Dashboard Preview

> Three Chart.js charts generated from your real transaction data:
> - 🥧 **Pie** — spending by category
> - 📊 **Bar** — month-over-month totals
> - 📈 **Line** — daily spending trend

Open `charts/index.html` in any browser after a run.

---

## 🗂️ Project Structure

```
FinanceParser/
├── src/
│   ├── Main.java                     # Entry point — orchestrates the full pipeline
│   ├── db/
│   │   ├── DBConnection.java         # Singleton JDBC connection (MySQL)
│   │   ├── SmsDAO.java               # SMS ingestion: raw → parsed → linked to transaction
│   │   └── TransactionDAO.java       # TRANSACTION table CRUD + merchant lookup
│   ├── model/
│   │   ├── Transaction.java          # POJO mirroring TRANSACTION table
│   │   ├── Category.java             # POJO mirroring CATEGORY table
│   │   └── Merchant.java             # POJO mirroring MERCHANT table
│   ├── parser/
│   │   ├── AxisSmsParser.java        # Axis Bank multi-line SMS parser
│   │   ├── BankSmsParser.java        # Generic single-line bank SMS parser
│   │   ├── CategoryEngine.java       # Keyword-based auto-categorization
│   │   ├── RegexParser.java          # Reusable regex utilities (amount, date, merchant)
│   │   ├── SmsFileReader.java        # Reads sms_inbox, splits on blank lines
│   │   ├── SmsXmlConverter.java      # Android XML backup → sms_inbox converter
│   │   └── FileReader.java           # Legacy classpath reader (not used in main flow)
│   └── report/
│       ├── ChartDataExporter.java    # Builds Chart.js JSON, writes HTML dashboard
│       └── QueryRunner.java          # Console reports: category, monthly, budget, merchants
├── resources/
│   └── sms_inbox                     # ← paste your bank SMS here
├── charts/
│   └── index.html                    # Auto-generated dashboard
└── lib/
    ├── mysql-connector-j-8.3.0.jar
    └── mysql-connector-j-9.6.0.jar
```

---

## ⚙️ Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 17+ |
| MySQL | 8.x |
| IntelliJ IDEA | Any recent version (recommended) |

---

## 🗄️ Database Setup

### Step 1 — Create tables

Run this in MySQL Workbench or the MySQL shell:

```sql
CREATE DATABASE IF NOT EXISTS finance_db;
USE finance_db;

CREATE TABLE IF NOT EXISTS USER (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100),
    email     VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS INPUT_SOURCE (
    source_id   INT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS CATEGORY (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    description   TEXT
);

CREATE TABLE IF NOT EXISTS MERCHANT (
    merchant_id   INT AUTO_INCREMENT PRIMARY KEY,
    merchant_name VARCHAR(255),
    merchant_type VARCHAR(100),
    location      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS BANK_SMS (
    sms_id         INT AUTO_INCREMENT PRIMARY KEY,
    raw_sms        TEXT,
    currency       VARCHAR(10),
    amount         DOUBLE,
    tx_type        VARCHAR(20),
    account_last4  VARCHAR(10),
    tx_date        DATE,
    tx_time        TIME,
    upi_type       VARCHAR(20),
    upi_ref        VARCHAR(100),
    merchant_raw   VARCHAR(255),
    merchant_clean VARCHAR(255),
    bank_name      VARCHAR(100),
    transaction_id INT,
    parse_status   ENUM('pending','parsed','failed') DEFAULT 'pending',
    failure_reason TEXT
);

CREATE TABLE IF NOT EXISTS TRANSACTION (
    transaction_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id          INT,
    merchant_id      INT,
    category_id      INT,
    source_id        INT,
    amount           DOUBLE,
    transaction_date DATE,
    currency         VARCHAR(10),
    status           VARCHAR(50),
    raw_text         TEXT
);

CREATE TABLE IF NOT EXISTS CATEGORY_RULES (
    rule_id     INT AUTO_INCREMENT PRIMARY KEY,
    keyword     VARCHAR(100),
    category_id INT,
    match_type  ENUM('CONTAINS','EXACT','STARTS') DEFAULT 'CONTAINS',
    priority    INT DEFAULT 100,
    is_active   BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS BUDGET (
    budget_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT,
    category_id  INT,
    limit_amount DOUBLE,
    period       VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS CHART_DATA (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT,
    chart_type VARCHAR(50),
    period     VARCHAR(20),
    data_json  LONGTEXT
);

CREATE TABLE IF NOT EXISTS PARSED_LOG (
    log_id          INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  INT,
    original_text   TEXT,
    matched_pattern VARCHAR(255),
    success         BOOLEAN
);
```

### Step 2 — Seed lookup data

```sql
-- Required: user_id=1 and source_id=1 are hardcoded in Main.java
INSERT IGNORE INTO USER (user_id, name, email) VALUES (1, 'Your Name', 'you@email.com');
INSERT IGNORE INTO INPUT_SOURCE (source_id, source_name) VALUES (1, 'SMS');

-- Categories
INSERT INTO CATEGORY (category_name, description) VALUES
('General',       'Uncategorized'),
('Food',          'Restaurants, food delivery'),
('Shopping',      'E-commerce and retail'),
('Entertainment', 'OTT, movies, gaming'),
('Transport',     'Cab, fuel, metro'),
('Utilities',     'Electricity, internet, phone'),
('Health',        'Pharmacy, clinic, hospital'),
('Travel',        'Hotels, flights');

-- Categorization rules
INSERT INTO CATEGORY_RULES (keyword, category_id, match_type, priority) VALUES
('swiggy',   2, 'CONTAINS', 10),
('zomato',   2, 'CONTAINS', 10),
('amazon',   3, 'CONTAINS', 20),
('flipkart', 3, 'CONTAINS', 20),
('netflix',  4, 'CONTAINS', 30),
('spotify',  4, 'CONTAINS', 30),
('uber',     5, 'CONTAINS', 40),
('ola',      5, 'CONTAINS', 40);

-- Sample budget
INSERT INTO BUDGET (user_id, category_id, limit_amount, period) VALUES
(1, 2, 3000, '2024-10'),
(1, 3, 5000, '2024-10'),
(1, 4, 1000, '2024-10');
```

---

## 🔧 Configuration

### 1. Database credentials

`src/db/DBConnection.java`:

```java
private static final String DB_URL      = "jdbc:mysql://localhost:3306/finance_db?useSSL=false&allowPublicKeyRetrieval=true";
private static final String DB_USER     = "root";
private static final String DB_PASSWORD = "your_password_here";
```

### 2. SMS file path

`src/Main.java`:

```java
// Windows
String smsFilePath = "C:\\Users\\YourName\\Desktop\\FinanceParser\\FinanceParser\\resources\\sms_inbox";

// Mac / Linux
String smsFilePath = "/home/yourname/FinanceParser/resources/sms_inbox";
```

### 3. Dashboard output path

Also in `Main.java`:

```java
ChartDataExporter.writeHtmlDashboard(
    DEFAULT_USER_ID,
    "C:\\Users\\YourName\\Desktop\\FinanceParser\\charts\\index.html"
);
```

---

## 📄 SMS Input Format

Edit `resources/sms_inbox`. Each message is one block separated by a **blank line**. Lines starting with `#` are comments.

**Axis Bank (multi-line):**
```
INR 72.00 debited
A/c no. XX8814
07-04-26, 21:27:59
UPI/P2M/646304856626/SAI BHAKRI CENTRE
Not you? SMS BLOCKUPI Cust ID to 919951860002
Axis Bank

INR 499.00 debited
A/c no. XX8814
10-04-26, 19:22:05
UPI/P2M/223344556677/SWIGGY
Not you? SMS BLOCKUPI Cust ID to 919951860002
Axis Bank
```

**Other banks (single-line):**
```
Rs.499.00 debited from ac **6789 on 15-10-24. UPI:123456789012. Avl bal:Rs.12,340.50-SBI

Rs 2399 debited from HDFC Bank A/c **1234 on 18/10/24 by AMAZON. Avl Bal: Rs 8,901.00

ICICI Bank Acct XX5678 debited for Rs 649.00 on 16-Oct-24. Info: NETFLIX VPA xyz@icici
```

**Importing from an Android SMS backup XML** (e.g., SMS Backup & Restore app):

```java
SmsXmlConverter.convert("path/to/backup.xml", "resources/sms_inbox");
```

---

## ▶️ Running

### IntelliJ IDEA

1. Open the `FinanceParser` folder as a project
2. **File → Project Structure → Libraries → +** → add both JARs from `lib/`
3. Set `Main` as the run configuration class
4. Run — console output shows parse results; dashboard is written to `charts/index.html`

### Command line

```bash
# Compile
javac -cp "lib/mysql-connector-j-9.6.0.jar" -d out src/**/*.java

# Run (Linux/Mac)
java -cp "out:lib/mysql-connector-j-9.6.0.jar" Main

# Run (Windows)
java -cp "out;lib/mysql-connector-j-9.6.0.jar" Main
```

---

## 🔄 How It Works

```
resources/sms_inbox
        │
        ▼
  SmsFileReader        splits file into SMS blocks (blank-line delimited)
        │
        ▼
  Bank detection       Axis multi-line  OR  single-line?
        │
   ┌────┴────┐
   ▼         ▼
AxisSmsParser  BankSmsParser
   └────┬────┘
        ▼
  Duplicate check      upi_ref already in BANK_SMS? → skip
        │
        ▼
  CategoryEngine       keyword rules → category_id
        │
        ▼
  TransactionDAO       findOrCreateMerchant + insertTransaction
        │
        ▼
  SmsDAO               insertRawSms (pending) → updateBankSms (parsed/failed)
        │
        ▼
  QueryRunner          console reports
        │
        ▼
  ChartDataExporter    DB → JSON → charts/index.html
```

---

## 📋 Console Reports

Printed to stdout on every run:

| Report | Details |
|---|---|
| Spending by Category | Ranked total per category |
| Monthly Trend | Transaction count + total per `YYYY-MM` |
| Budget vs Actual | Spend vs limit; ⚠ flags over-budget |
| Top 5 Merchants | Ranked by total spend |
| DEBIT vs CREDIT | Count and total for each type |

---

## 🧩 Extending

**New bank** — Add a parser class with `parse(String rawSms)` returning `SmsResult`, then add a detection branch in `Main.main()`.

**New category rule** — `INSERT` into `CATEGORY_RULES`. Call `CategoryEngine.reloadRules()` to pick it up without restarting.

**New chart** — Add a `buildXxxChartJson(int userId)` method in `ChartDataExporter`, wire it into `exportAll()`, and add the `<canvas>` + `new Chart(...)` block in `writeHtmlDashboard()`.

---

## 🛠️ Troubleshooting

| Error | Fix |
|---|---|
| `MySQL JDBC driver not found` | Add JARs from `lib/` to classpath (IntelliJ: Project Structure → Libraries) |
| `No SMS messages found` | Check the path in `Main.java`; messages must be blank-line delimited |
| FK violation on first run | Run the seed SQL — `USER` and `INPUT_SOURCE` must have `id=1` before anything else |
| Dashboard shows "No Data" | No rows for `user_id=1` in `TRANSACTION`. Run the full pipeline with valid SMS input first |
| `ClassNotFoundException` on switch expression | Set SDK to Java 17+ in IntelliJ (Project Structure → Project) |

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Database | MySQL 8.x |
| DB Driver | mysql-connector-j 9.6.0 |
| Charts | Chart.js 4.4.0 (CDN) |
| Parsing | `java.util.regex` (zero external dependencies) |
| IDE | IntelliJ IDEA |
| Build | `javac` / IntelliJ (no build tool) |

---

## 📄 License

MIT — use freely, attribution appreciated.
