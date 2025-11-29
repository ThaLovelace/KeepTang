package com.example.keeptang.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "kapook.db";
    private static final int DATABASE_VERSION = 2; // ✅ อัปเดตเวอร์ชันเป็น 2

    // --- ตาราง 1: Categories ---
    public static final String TABLE_CATEGORIES = "CATEGORIES";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";
    public static final String COL_CAT_ICON = "icon_name";

    // --- ตาราง 2: Transactions ---
    public static final String TABLE_TRANSACTIONS = "TRANSACTIONS";
    public static final String COL_TR_ID = "id";
    public static final String COL_TR_NAME = "name";
    public static final String COL_TR_PRICE = "price";
    public static final String COL_TR_TIMESTAMP = "timestamp";
    public static final String COL_TR_CATEGORY_ID = "category_id";

    // --- ✅ ตาราง 3: AI Knowledge (ความจำสมองกล) ---
    public static final String TABLE_AI_KNOWLEDGE = "AI_KNOWLEDGE";
    public static final String COL_AI_WORD = "word";
    public static final String COL_AI_CAT_ID = "category_id";

    // SQL สร้างตาราง
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

    private static final String CREATE_TABLE_AI_KNOWLEDGE =
            "CREATE TABLE " + TABLE_AI_KNOWLEDGE + "(" +
                    COL_AI_WORD + " TEXT PRIMARY KEY," +
                    COL_AI_CAT_ID + " INTEGER" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_AI_KNOWLEDGE); // สร้างตาราง AI
        seedCategories(db);
    }

    private void seedCategories(SQLiteDatabase db) {
        String insertSQL = "INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ", " + COL_CAT_ICON + ") VALUES ";

        // --- รายจ่าย (Expense) ---
        db.execSQL(insertSQL + "('Food & Drink', 'icon_food');");       // ID 1
        db.execSQL(insertSQL + "('Travel', 'icon_travel');");           // ID 2
        db.execSQL(insertSQL + "('Shopping', 'icon_shopping');");       // ID 3
        db.execSQL(insertSQL + "('Bills', 'icon_bills');");             // ID 4
        db.execSQL(insertSQL + "('Entertainment', 'icon_entertainment');"); // ID 5
        db.execSQL(insertSQL + "('Health', 'icon_health');");           // ID 6
        db.execSQL(insertSQL + "('Home', 'icon_home');");               // ID 7
        db.execSQL(insertSQL + "('Education', 'icon_education');");     // ID 8
        db.execSQL(insertSQL + "('Gifts', 'icon_gifts');");             // ID 9

        // --- ✅ รายรับ (Income) ---
        db.execSQL(insertSQL + "('Salary', 'icon_income');");           // ID 10 (ใช้ไอคอนเดิมแก้ขัดไปก่อน)
        db.execSQL(insertSQL + "('Bonus', 'icon_point_coin');");        // ID 11
        db.execSQL(insertSQL + "('Investment', 'icon_chart');");        // ID 12
        db.execSQL(insertSQL + "('Other Income', 'icon_point_coin');"); // ID 13

        // --- Others ---
        db.execSQL(insertSQL + "('Others', 'icon_others');");           // ID 14
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AI_KNOWLEDGE);
        onCreate(db);
    }

    // --- ฟังก์ชันช่วยสำหรับ AI ---

    // 1. สอน AI (บันทึกคำตอบลงเครื่อง)
    public void teachAI(String word, int categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AI_WORD, word.toLowerCase().trim());
        values.put(COL_AI_CAT_ID, categoryId);
        db.insertWithOnConflict(TABLE_AI_KNOWLEDGE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // 2. ถามความจำ (ค้นหาในเครื่อง)
    public int askMemory(String word) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_AI_KNOWLEDGE, new String[]{COL_AI_CAT_ID},
                COL_AI_WORD + "=?", new String[]{word.toLowerCase().trim()}, null, null, null);

        int catId = -1; // -1 แปลว่าไม่เจอ
        if (cursor != null && cursor.moveToFirst()) {
            catId = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return catId;
    }

    // ✅ อัปเดตรายการ (Update)
    public void updateTransaction(int id, String name, double price, int categoryId, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TR_NAME, name);
        values.put(COL_TR_PRICE, price);
        values.put(COL_TR_CATEGORY_ID, categoryId);
        values.put(COL_TR_TIMESTAMP, timestamp);

        db.update(TABLE_TRANSACTIONS, values, COL_TR_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ✅ ลบรายการ (Delete)
    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COL_TR_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}