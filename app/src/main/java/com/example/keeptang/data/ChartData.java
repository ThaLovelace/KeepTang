package com.example.keeptang.data; // (Package 'data' ของคุณ)

public class ChartData {

    // (นี่คือ "3 คอลัมน์" (3 Columns) ที่เราต้องการ)
    private String categoryName;
    private double percentage;
    private double amount;
    private String categoryIconName; // (เรา "ต้องการ" (Need) "ไอคอน" (Icon) ... เพื่อ "สี" (Color) ของ Pie Chart)

    // Constructor (ตัวสร้าง)
    public ChartData(String categoryName, double percentage, double amount, String categoryIconName) {
        this.categoryName = categoryName;
        this.percentage = percentage;
        this.amount = amount;
        this.categoryIconName = categoryIconName;
    }

    // Getters (ตัวดึงข้อมูล)
    public String getCategoryName() { return categoryName; }
    public double getPercentage() { return percentage; }
    public double getAmount() { return amount; }
    public String getCategoryIconName() { return categoryIconName; }
}