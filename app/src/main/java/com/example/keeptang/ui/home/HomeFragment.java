package com.example.keeptang.ui.home;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler; // ✅ "Bug Fix": "Import" (Import) "ผู้ส่งสาร" (Messenger)
import android.os.Looper;  // ✅ "Bug Fix": "Import" (Import) "เธรดหลัก" (Main Thread)
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keeptang.R;
import com.example.keeptang.data.DatabaseHelper;
import com.example.keeptang.data.Transaction;
import com.example.keeptang.logic.Gamification;
import com.example.keeptang.ui.TransactionAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService; // ✅ "Bug Fix": "Import" (Import) "คนงาน" (Worker)
import java.util.concurrent.Executors; // ✅ "Bug Fix": "Import" (Import) "โรงงาน" (Factory)

public class HomeFragment extends Fragment {

    // 1. "ประกาศ" (Declare) "View" (UI)
    private TextView tvPoints;
    private TextView tvTotalBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private RecyclerView recyclerTransactions;

    // 2. "ประกาศ" (Declare) "Logic" (Logic)
    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;

    // ✅✅✅ "Bug Fix" (แก้ไข): "สร้าง" (Create) "เครื่องมือ" (Tools) "เธรด" (Threading) ✅✅✅
    private ExecutorService executor; // "คนงาน" (Worker) (สำหรับ "งานหนัก" (Heavy work))
    private Handler handler; // "ผู้ส่งสาร" (Messenger) (สำหรับ "ส่ง" (Send) "ผลลัพธ์" (Result) "กลับ" (Back) มา "หน้าจอ" (UI))

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 4. "เชื่อม" (Connect) "Logic" (Logic)
        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        transactionList = new ArrayList<>();

        // ✅ "Bug Fix": "สร้าง" (Initialize) "เครื่องมือ" (Tools) "เธรด" (Threading)
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        // 5. "เชื่อม" (Connect) "View" (UI)
        tvPoints = view.findViewById(R.id.tv_points);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        recyclerTransactions = view.findViewById(R.id.recycler_transactions);

        // 6. "ตั้งค่า" (Setup) "RecyclerView" (RecyclerView)
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        recyclerTransactions.setAdapter(transactionAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // --- "Day 3 - Task 2" (Logic) (ฉบับ "Fix" ANR) ---

        // (เราจะ "ไม่" (NOT) "ทำ" (Do) "งานหนัก" (Heavy work) ที่นี่ "อีกแล้ว" (Anymore))

        // ✅ "Bug Fix": "ส่ง" (Execute) "ทุกอย่าง" (Everything) ...
        // ... ไป "รัน" (Run) บน "เธรดเบื้องหลัง" (Background Thread) ...
        // ... โดย "คนงาน" (Worker) 'executor'
        executor.execute(() -> {

            // --- (นี่คือ "โค้ด" (Code) ที่ "รัน" (Running) "เบื้องหลัง" (In Background)) ---

            // 1. "รัน" (Run) "Logic เช็คอิน" (Check-in Logic)!
            int currentPoints = gamificationLogic.performDailyCheckIn();

            // 2. "โหลด" (Load) "ข้อมูล" (Data) "สรุปยอด" (Summary) (จาก "Database" (DB))
            SummaryData summary = loadSummaryData();

            // 3. "โหลด" (Load) "ข้อมูล" (Data) "ลิสต์" (List) (จาก "Database" (DB))
            List<Transaction> recentTransactions = loadRecentTransactions();

            // 4. "ส่ง" (Send) "ผลลัพธ์" (Results) ...
            // ... "กลับไป" (Back) ให้ "เธรด UI" (UI Thread) ...
            // ... โดย "ผู้ส่งสาร" (Messenger) 'handler'
            handler.post(() -> {

                // --- (นี่คือ "โค้ด" (Code) ที่ "กลับมา" (Back) "รัน" (Run) บน "เธรด UI" (UI Thread)) ---
                // (เรา "ปลอดภัย" (Safe) ที่จะ "แตะ" (Touch) "หน้าจอ" (UI) แล้ว)

                // A. "อัปเดต" (Update) "แต้ม" (Points)
                tvPoints.setText(String.valueOf(currentPoints));

                // B. "อัปเดต" (Update) "สรุปยอด" (Summary)
                tvTotalBalance.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", summary.totalBalance));
                tvTotalIncome.setText("+ ฿ " + String.format(Locale.getDefault(), "%,.2f", summary.totalIncome));
                tvTotalExpense.setText("- ฿ " + String.format(Locale.getDefault(), "%,.2f", Math.abs(summary.totalExpense)));

                // C. "อัปเดต" (Update) "ลิสต์" (List)
                transactionList.clear();
                transactionList.addAll(recentTransactions);
                transactionAdapter.notifyDataSetChanged();
            });
        });
    }

    /**
     * "ดึง" (Fetch) "ข้อมูล" (Data) "สรุปยอด" (Summary Box)
     * (*** "ห้าม" (MUST NOT) "เรียก" (Call) "เมธอด" (Method) นี้ "ตรงๆ" (Directly) "จาก 'onResume'" (From 'onResume') ***)
     */
    private SummaryData loadSummaryData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double totalIncome = 0;
        double totalExpense = 0;

        Cursor incomeCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COL_TR_PRICE + " > 0", null);
        if (incomeCursor.moveToFirst()) {
            totalIncome = incomeCursor.getDouble(0);
        }
        incomeCursor.close();

        Cursor expenseCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COL_TR_PRICE + " < 0", null);
        if (expenseCursor.moveToFirst()) {
            totalExpense = expenseCursor.getDouble(0);
        }
        expenseCursor.close();

        // "คืนค่า" (Return) "ผลลัพธ์" (Result)
        return new SummaryData(totalIncome, totalExpense, (totalIncome + totalExpense));
    }

    /**
     * "ดึง" (Fetch) "ข้อมูล" (Data) "รายการล่าสุด" (Recent List)
     * (*** "ห้าม" (MUST NOT) "เรียก" (Call) "เมธอด" (Method) นี้ "ตรงๆ" (Directly) "จาก 'onResume'" (From 'onResume') ***)
     */
    private List<Transaction> loadRecentTransactions() {
        List<Transaction> newList = new ArrayList<>(); // "สร้าง" (Create) "List ใหม่" (New List)
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT T.*, C." + DatabaseHelper.COL_CAT_NAME + ", C." + DatabaseHelper.COL_CAT_ICON +
                " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " T" +
                " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " C ON T." + DatabaseHelper.COL_TR_CATEGORY_ID + " = C." + DatabaseHelper.COL_CAT_ID +
                " ORDER BY T." + DatabaseHelper.COL_TR_TIMESTAMP + " DESC" +
                " LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_NAME));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_PRICE));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_TIMESTAMP));
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_CATEGORY_ID));
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
            String categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));

            newList.add(new Transaction(id, name, price, timestamp, categoryId, categoryName, categoryIconName));
        }
        cursor.close();

        // "คืนค่า" (Return) "ผลลัพธ์" (Result)
        return newList;
    }

    /**
     * "ผู้ช่วย" (Helper) "Class" (Class) "จิ๋ว" (Tiny) ...
     * ... (ใช้ "ห่อ" (Wrap) "ผลลัพธ์" (Result) 3 ค่า... จาก "Background Thread" (Background Thread))
     */
    private static class SummaryData {
        final double totalIncome;
        final double totalExpense;
        final double totalBalance;

        SummaryData(double totalIncome, double totalExpense, double totalBalance) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.totalBalance = totalBalance;
        }
    }

}