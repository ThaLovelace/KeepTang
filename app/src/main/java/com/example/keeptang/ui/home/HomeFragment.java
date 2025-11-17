package com.example.keeptang.ui.home;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView; // ✅ Import เพิ่ม
import android.widget.Spinner;     // ✅ Import เพิ่ม
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

public class HomeFragment extends Fragment {

    private TextView tvPoints;
    private TextView tvTotalBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private RecyclerView recyclerTransactions;

    // ✅ ตัวแปรสำหรับตัวกรอง
    private Spinner spinnerFilter;
    private String startDateStr = "";
    private String endDateStr = "";

    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;

    private ExecutorService executor;
    private Handler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        transactionList = new ArrayList<>();

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        // เชื่อม View
        tvPoints = view.findViewById(R.id.tv_points);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        recyclerTransactions = view.findViewById(R.id.recycler_transactions);
        spinnerFilter = view.findViewById(R.id.spinner_filter); // ✅ เชื่อม Dropdown

        recyclerTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        recyclerTransactions.setAdapter(transactionAdapter);

        // ✅ ตั้งค่าการทำงานของตัวกรอง
        setupFilterSpinner();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // (เราย้ายการโหลดข้อมูลไปไว้ที่ setupFilterSpinner แล้ว เพื่อไม่ให้โหลดซ้ำ)
    }

    // ✅ ฟังก์ชันจัดการตัวเลือก Filter
    private void setupFilterSpinner() {
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 0=This Month, 1=This Week, 2=Today, 3=Custom/All (ตามลำดับใน strings.xml)
                calculateDateRange(position);
                refreshData(); // โหลดข้อมูลใหม่ตามช่วงเวลา
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ✅ ฟังก์ชันคำนวณวันเริ่มต้น-สิ้นสุด
    private void calculateDateRange(int filterIndex) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(calendar.getTime());

        if (filterIndex == 0) { // This Month
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            startDateStr = sdf.format(calendar.getTime()) + " 00:00:00";

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            endDateStr = sdf.format(calendar.getTime()) + " 23:59:59";

        } else if (filterIndex == 1) { // This Week
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            startDateStr = sdf.format(calendar.getTime()) + " 00:00:00";

            calendar.add(Calendar.DAY_OF_WEEK, 6);
            endDateStr = sdf.format(calendar.getTime()) + " 23:59:59";

        } else if (filterIndex == 2) { // Today
            startDateStr = todayDate + " 00:00:00";
            endDateStr = todayDate + " 23:59:59";

        } else { // Custom / All Time
            startDateStr = ""; // ไม่กรองวันที่
            endDateStr = "";
        }
    }

    private void refreshData() {
        executor.execute(() -> {
            int currentPoints = gamificationLogic.performDailyCheckIn();
            SummaryData summary = loadSummaryData();
            List<Transaction> recentTransactions = loadRecentTransactions();

            handler.post(() -> {
                if (getContext() == null) return;

                tvPoints.setText(String.valueOf(currentPoints));

                tvTotalBalance.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", summary.totalBalance));
                tvTotalIncome.setText("+ ฿ " + String.format(Locale.getDefault(), "%,.2f", summary.totalIncome));
                tvTotalExpense.setText("- ฿ " + String.format(Locale.getDefault(), "%,.2f", Math.abs(summary.totalExpense)));

                transactionList.clear();
                transactionList.addAll(recentTransactions);
                transactionAdapter.notifyDataSetChanged();
            });
        });
    }

    private SummaryData loadSummaryData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double totalIncome = 0;
        double totalExpense = 0;

        // ✅ สร้างเงื่อนไข WHERE ตามวันที่
        String dateCondition = "";
        String[] args = null;
        if (!startDateStr.isEmpty()) {
            dateCondition = " AND " + DatabaseHelper.COL_TR_TIMESTAMP + " BETWEEN ? AND ?";
            args = new String[]{startDateStr, endDateStr};
        }

        // Query รายรับ
        Cursor incomeCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COL_TR_PRICE + " > 0" + dateCondition, args);
        if (incomeCursor.moveToFirst()) {
            totalIncome = incomeCursor.getDouble(0);
        }
        incomeCursor.close();

        // Query รายจ่าย
        Cursor expenseCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COL_TR_PRICE + " < 0" + dateCondition, args);
        if (expenseCursor.moveToFirst()) {
            totalExpense = expenseCursor.getDouble(0);
        }
        expenseCursor.close();

        return new SummaryData(totalIncome, totalExpense, (totalIncome + totalExpense));
    }

    private List<Transaction> loadRecentTransactions() {
        List<Transaction> newList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String whereClause = "";
        String[] args = null;
        if (!startDateStr.isEmpty()) {
            whereClause = " WHERE T." + DatabaseHelper.COL_TR_TIMESTAMP + " BETWEEN ? AND ?";
            args = new String[]{startDateStr, endDateStr};
        }

        // ✅✅✅ แก้ไข SQL: ตั้งชื่อเล่น (AS category_name) ให้ชื่อหมวดหมู่ เพื่อไม่ให้ซ้ำกับชื่อรายการ
        String query = "SELECT T.*, C." + DatabaseHelper.COL_CAT_NAME + " AS category_name, C." + DatabaseHelper.COL_CAT_ICON +
                " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " T" +
                " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " C ON T." + DatabaseHelper.COL_TR_CATEGORY_ID + " = C." + DatabaseHelper.COL_CAT_ID +
                whereClause +
                " ORDER BY T." + DatabaseHelper.COL_TR_TIMESTAMP + " DESC" +
                " LIMIT 20";

        Cursor cursor = db.rawQuery(query, args);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_ID));
            // อันนี้ดึง 'name' ของรายการ (Transaction)
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_NAME));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_PRICE));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_TIMESTAMP));
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TR_CATEGORY_ID));

            // ✅✅✅ แก้ไขการดึงค่า: ใช้ชื่อเล่น 'category_name' ที่ตั้งไว้
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));

            String categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));

            newList.add(new Transaction(id, name, price, timestamp, categoryId, categoryName, categoryIconName));
        }
        cursor.close();

        return newList;
    }

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