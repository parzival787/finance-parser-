package model;
// Category — POJO mirroring the CATEGORY table
public class Category {
    private int    categoryId;
    private String categoryName;
    private String description;

    public Category(int categoryId, String categoryName, String description) {
        this.categoryId=categoryId; this.categoryName=categoryName; this.description=description;
    }
    public int    getCategoryId()   { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getDescription()  { return description; }

    @Override public String toString() {
        return "[Category] id=" + categoryId + " name=" + categoryName;
    }
}
