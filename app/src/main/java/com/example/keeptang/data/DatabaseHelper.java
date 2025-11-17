package com.example.keeptang.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // --- นี่คือ "พิมพ์เขียว" (Schema) ที่เราออกแบบ ---

    // Database Info
    private static final String DATABASE_NAME = "kapook.db";
    private static final int DATABASE_VERSION = 1;

    // ตารางที่ 1: Categories (ตามลิสต์ 11 หมวดหมู่)
    public static final String TABLE_CATEGORIES = "CATEGORIES";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";
    public static final String COL_CAT_ICON = "icon_name";

    // ตารางที่ 2: Transactions
    public static final String TABLE_TRANSACTIONS = "TRANSACTIONS";
    public static final String COL_TR_ID = "id";
    public static final String COL_TR_NAME = "name";
    public static final String COL_TR_PRICE = "price";
    public static final String COL_TR_TIMESTAMP = "timestamp";
    public static final String COL_TR_CATEGORY_ID = "category_id";


    // --- โค้ดสร้างตาราง (SQL) ---

    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE " + TABLE_CATEGORIES + "(" +
                    COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COL_CAT_NAME + " TEXT NOT NULL UNIQUE," +
                    COL_CAT_ICON + " TEXT NOT NULL" +
                    ")";

    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + "(" +
                    COL_TR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COL_TR_NAME + " TEXT NOT NULL," +
                    COL_TR_PRICE + " REAL NOT NULL," +
                    COL_TR_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    COL_TR_CATEGORY_ID + " INTEGER," +
                    "FOREIGN KEY(" + COL_TR_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COL_CAT_ID + ")" +
                    ")";


    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. สร้าง 2 ตาราง
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);

        // 2. (สำคัญ!) "ใส่ข้อมูลตั้งต้น" (Seed Data) 11 หมวดหมู่
        // (เราจะ "Hardcode" ลิสต์ไอคอน 11 อัน ลงไปเลย)
        seedCategories(db);
    }

    private void seedCategories(SQLiteDatabase db) {
        String insertSQL = "INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ", " + COL_CAT_ICON + ") VALUES ";

        db.execSQL(insertSQL + "('Food & Drink', 'icon_food');");
        db.execSQL(insertSQL + "('Travel', 'icon_travel');");
        db.execSQL(insertSQL + "('Shopping', 'icon_shopping');");
        db.execSQL(insertSQL + "('Bills', 'icon_bills');");
        db.execSQL(insertSQL + "('Entertainment', 'icon_entertainment');");
        db.execSQL(insertSQL + "('Health', 'icon_health');");
        db.execSQL(insertSQL + "('Home', 'icon_home');");
        db.execSQL(insertSQL + "('Education', 'icon_education');");
        db.execSQL(insertSQL + "('Gifts', 'icon_gifts');");

        // (หมวดหมู่พิเศษ)
        db.execSQL(insertSQL + "('Income', 'icon_income');");
        db.execSQL(insertSQL + "('Others', 'icon_others');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // (สำหรับ MVP 6 วัน... เราจะ "ลบ" ทิ้งแล้วสร้างใหม่)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }
}
