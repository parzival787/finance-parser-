package model;
// Merchant — POJO mirroring the MERCHANT table
public class Merchant {
    private int    merchantId;
    private String merchantName;
    private String merchantType;
    private String location;

    public Merchant(int merchantId, String merchantName, String merchantType, String location) {
        this.merchantId=merchantId; this.merchantName=merchantName;
        this.merchantType=merchantType; this.location=location;
    }
    // Getters
    public int    getMerchantId()   { return merchantId; }
    public String getMerchantName() { return merchantName; }
    public String getMerchantType() { return merchantType; }
    public String getLocation()     { return location; }

    @Override public String toString() {
        return "[Merchant] id=" + merchantId + " name=" + merchantName;
    }
}
