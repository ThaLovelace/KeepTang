package com.example.keeptang.ui.calendar;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.keeptang.data.DatabaseHelper;
import com.example.keeptang.data.Transaction;
import com.example.keeptang.logic.Gamification;
import com.example.keeptang.ui.TransactionAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarFragment extends Fragment {

    private TextView tvPoints;
    private TextView tvCalendarTotal;
    private CalendarView calendarView;
    private RecyclerView recyclerTransactions;

    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private String selectedDate;

    // เครื่องมือสำหรับ Background Thread
    private ExecutorService executor;
    private Handler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        transactionList = new ArrayList<>();

        // สร้าง Executor และ Handler
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        tvPoints = view.findViewById(R.id.tv_points_calendar);
        tvCalendarTotal = view.findViewById(R.id.tv_calendar_total);
        calendarView = view.findViewById(R.id.calendar_view);
        recyclerTransactions = view.findViewById(R.id.recycler_transactions_calendar);

        recyclerTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        recyclerTransactions.setAdapter(transactionAdapter);

        // ตั้งค่าวันที่เริ่มต้นเป็นวันนี้
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        setupCalendarListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // เรียกโหลดข้อมูลใน Background Thread
        refreshData();
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

            // เมื่อเปลี่ยนวันที่ ก็เรียกโหลดข้อมูลใหม่
            refreshData();
        });
    }

    private void refreshData() {
        executor.execute(() -> {
            // 1. ทำงานหนัก (Query Database) ใน Background
            int currentPoints = gamificationLogic.performDailyCheckIn();
            CalendarData data = loadDataForSelectedDate();

            // 2. ส่งผลลัพธ์กลับมาอัปเดต UI ใน Main Thread
            handler.post(() -> {
                if (getContext() == null) return;

                tvPoints.setText(String.valueOf(currentPoints));
                tvCalendarTotal.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", data.dailyTotal));

                transactionList.clear();
                transactionList.addAll(data.newList);
                transactionAdapter.notifyDataSetChanged();
            });
        });
    }

    private CalendarData loadDataForSelectedDate() {
        if (getContext() == null) {
            return new CalendarData(0, new ArrayList<>());
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Transaction> newList = new ArrayList<>();
        double dailyTotal = 0;

        // --- Query 1: สรุปยอด ---
        Cursor summaryCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE DATE(" + DatabaseHelper.COL_TR_TIMESTAMP + ") = ?",
                new String[]{selectedDate}
        );
        if (summaryCursor.moveToFirst()) {
            dailyTotal = summaryCursor.getDouble(0);
        }
        summaryCursor.close();

        // --- Query 2: ลิสต์รายการ ---
        // ✅ แก้ไข: เพิ่ม AS category_name เพื่อไม่ให้ชื่อซ้ำ
        String query = "SELECT T.*, C." + DatabaseHelper.COL_CAT_NAME + " AS category_name, C." + DatabaseHelper.COL_CAT_ICON +
                " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " T" +
                " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " C ON T." + DatabaseHelper.COL_TR_CATEGORY_ID + " = C." + DatabaseHelper.COL_CAT_ID +
                " WHERE DATE(T." + DatabaseHelper.COL_TR_TIMESTAMP + ") = ?" +
                " ORDER BY T." + DatabaseHelper.COL_TR_TIMESTAMP + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{selectedDate});

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_NAME));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_PRICE));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_TIMESTAMP));
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_CATEGORY_ID));

            // ✅ แก้ไข: ดึงค่าจาก category_name
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
            String categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));

            newList.add(new Transaction(id, name, price, timestamp, categoryId, categoryName, categoryIconName));
        }
        cursor.close();

        return new CalendarData(dailyTotal, newList);
    }

    private static class CalendarData {
        final double dailyTotal;
        final List<Transaction> newList;

        CalendarData(double dailyTotal, List<Transaction> newList) {
            this.dailyTotal = dailyTotal;
            this.newList = newList;
        }
    }
}