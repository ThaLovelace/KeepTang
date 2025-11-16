package com.example.keeptang.data; // (Package 'data' ของคุณ)

public class Transaction {
    private int id;
    private String name;
    private double price;
    private String timestamp;
    private int categoryId;

    // (เราจะ "เพิ่ม" (Add) 2 field นี้...
    // ... เพื่อ "เก็บ" (Hold) "ข้อมูล" (Data) ที่ "Join" (Joined) มาจาก "ตาราง CATEGORIES")
    private String categoryName;
    private String categoryIconName;

    // Constructor (ตัวสร้าง) (สำหรับ "ดึง" (Fetch) ข้อมูลจาก DB)
    public Transaction(int id, String name, double price, String timestamp, int categoryId, String categoryName, String categoryIconName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.timestamp = timestamp;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryIconName = categoryIconName;
    }

    // Getters (ตัวดึงข้อมูล) (ที่ "Adapter" จะ "เรียก" (Call) ใช้)
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getTimestamp() { return timestamp; }
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryIconName() { return categoryIconName; }
}
