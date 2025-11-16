package com.example.keeptang.ui.calendar; // (Package 'ui.calendar' ของคุณ)

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler; // ✅ "Bug Fix" (แก้ ANR): "Import" (Import) "ผู้ส่งสาร" (Messenger)
import android.os.Looper;  // ✅ "Bug Fix" (แก้ ANR): "Import" (Import) "เธรดหลัก" (Main Thread)
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keeptang.R;
import com.example.keeptang.data.DatabaseHelper; // (Import 'data')
import com.example.keeptang.data.Transaction; // (Import 'Transaction')
import com.example.keeptang.logic.Gamification; // (Import 'logic')
import com.example.keeptang.ui.TransactionAdapter; // (Import "ผู้ช่วย List")

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService; // ✅ "Bug Fix" (แก้ ANR): "Import" (Import) "คนงาน" (Worker)
import java.util.concurrent.Executors; // ✅ "Bug Fix" (แก้ ANR): "Import" (Import) "โรงงาน" (Factory)

public class CalendarFragment extends Fragment {

    // 1. "ประกาศ" (Declare) "View" (UI)
    private TextView tvPoints;
    private TextView tvCalendarTotal;
    private CalendarView calendarView;
    private RecyclerView recyclerTransactions;

    // 2. "ประกาศ" (Declare) "Logic" (Logic)
    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private String selectedDate;

    // ✅ "Bug Fix" (แก้ ANR): "สร้าง" (Create) "เครื่องมือ" (Tools) "เธรด" (Threading)
    private ExecutorService executor;
    private Handler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // 4. "เชื่อม" (Connect) "Logic" (Logic)
        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        transactionList = new ArrayList<>();

        // ✅ "Bug Fix" (แก้ ANR): "สร้าง" (Initialize) "เครื่องมือ" (Tools) "เธรด" (Threading)
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        // 5. "เชื่อม" (Connect) "View" (UI)
        tvPoints = view.findViewById(R.id.tv_points_calendar);
        tvCalendarTotal = view.findViewById(R.id.tv_calendar_total);
        calendarView = view.findViewById(R.id.calendar_view);
        recyclerTransactions = view.findViewById(R.id.recycler_transactions_calendar);

        // 6. "ตั้งค่า" (Setup) "RecyclerView" (RecyclerView)
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        recyclerTransactions.setAdapter(transactionAdapter);

        // 7. "ตั้งค่า" (Setup) "วันที่" (Date) "เริ่มต้น" (Default)
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // 8. "ติดตั้ง" (Setup) "ตัวดักฟัง" (Listener) "ปฏิทิน" (Calendar)
        setupCalendarListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // ✅ "Bug Fix" (แก้ ANR): "ส่ง" (Execute) "งานหนัก" (Heavy work) ...
        // ... ไป "รัน" (Run) "เบื้องหลัง" (Background Thread)
        executor.execute(() -> {

            // --- (นี่คือ "โค้ด" (Code) ที่ "รัน" (Running) "เบื้องหลัง" (In Background)) ---

            // 1. "รัน" (Run) "Logic เช็คอิน" (Check-in Logic)!
            int currentPoints = gamificationLogic.performDailyCheckIn();

            // 2. "โหลด" (Load) "ข้อมูล" (Data) "สรุปยอด" (Summary) (จาก "Database" (DB))
            CalendarData data = loadDataForSelectedDate();

            // 3. "ส่ง" (Send) "ผลลัพธ์" (Results) ...
            // ... "กลับไป" (Back) ให้ "เธรด UI" (UI Thread) ...
            handler.post(() -> {

                // --- (นี่คือ "โค้ด" (Code) ที่ "กลับมา" (Back) "รัน" (Run) บน "เธรด UI" (UI Thread)) ---

                // A. "อัปเดต" (Update) "แต้ม" (Points)
                tvPoints.setText(String.valueOf(currentPoints));

                // B. "อัปเดต" (Update) "สรุปยอด" (Summary)
                tvCalendarTotal.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", data.dailyTotal));

                // C. "อัปเดต" (Update) "ลิสต์" (List)
                transactionList.clear();
                transactionList.addAll(data.newList);
                transactionAdapter.notifyDataSetChanged();
            });
        });
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

            // "โหลด" (Load) "ข้อมูล" (Data) "ใหม่" (New)
            // ✅ "Bug Fix" (แก้ ANR): "ต้อง" (Must) "รัน" (Run) "เบื้องหลัง" (Background) "ด้วย" (Too)!
            executor.execute(() -> {
                CalendarData data = loadDataForSelectedDate();
                handler.post(() -> {
                    tvCalendarTotal.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", data.dailyTotal));
                    transactionList.clear();
                    transactionList.addAll(data.newList);
                    transactionAdapter.notifyDataSetChanged();
                });
            });
        });
    }

    /**
     * "โหลด" (Load) "ข้อมูล" (Data) "สรุปยอด" (Summary) และ "ลิสต์" (List)
     * (*** "ห้าม" (MUST NOT) "เรียก" (Call) "เมธอด" (Method) นี้ "ตรงๆ" (Directly) "จาก 'onResume'" (From 'onResume') ***)
     */
    private CalendarData loadDataForSelectedDate() {
        if (getContext() == null) {
            return new CalendarData(0, new ArrayList<>()); // (คืนค่า "ว่าง" (Empty) ถ้า "พัง" (Crash))
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Transaction> newList = new ArrayList<>(); // "สร้าง" (Create) "List ใหม่" (New List)

        // --- "Query" (Query) "สรุปยอด" (Summary) (เฉพาะ "วัน" (Date) นี้) ---
        double dailyTotal = 0;
        Cursor summaryCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE DATE(" + DatabaseHelper.COL_TR_TIMESTAMP + ") = ?",
                new String[]{selectedDate}
        );
        if (summaryCursor.moveToFirst()) {
            dailyTotal = summaryCursor.getDouble(0);
        }
        summaryCursor.close();

        // --- "Query" (Query) "ลิสต์รายการ" (Transaction List) (เฉพาะ "วัน" (Date) นี้) ---
        String query = "SELECT T.*, C." + DatabaseHelper.COL_CAT_NAME + ", C." + DatabaseHelper.COL_CAT_ICON +
                " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " T" +
                " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " C ON T." + DatabaseHelper.COL_TR_CATEGORY_ID + " = C." + DatabaseHelper.COL_CAT_ID +
                " WHERE DATE(T." + DatabaseHelper.COL_TR_TIMESTAMP + ") = ?" +
                " ORDER BY T." + DatabaseHelper.COL_TR_TIMESTAMP + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{selectedDate});

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_NAME));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_PRICE)); // (✅ Bug Fix: แก้ 'DatabaseGHelper')
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_TIMESTAMP));
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_CATEGORY_ID));
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
            String categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));

            newList.add(new Transaction(id, name, price, timestamp, categoryId, categoryName, categoryIconName));
        }
        cursor.close();

        // "คืนค่า" (Return) "ผลลัพธ์" (Result) (2 อย่าง)
        return new CalendarData(dailyTotal, newList);
    }

    /**
     * "ผู้ช่วย" (Helper) "Class" (Class) "จิ๋ว" (Tiny) ...
     * ... (ใช้ "ห่อ" (Wrap) "ผลลัพธ์" (Result) 2 ค่า... จาก "Background Thread" (Background Thread))
     */
    private static class CalendarData {
        final double dailyTotal;
        final List<Transaction> newList;

        CalendarData(double dailyTotal, List<Transaction> newList) {
            this.dailyTotal = dailyTotal;
            this.newList = newList;
        }
    }
}