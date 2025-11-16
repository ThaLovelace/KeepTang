package com.example.keeptang.data; // (หรือ Package name ของคุณ)

public class Category {
    private int id;
    private String name;
    private String iconName;

    // Constructor (ตัวสร้าง)
    public Category(int id, String name, String iconName) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
    }

    // Getters (ตัวดึงข้อมูล)
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconName() {
        return iconName;
    }
}