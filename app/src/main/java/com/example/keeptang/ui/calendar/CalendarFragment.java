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

    // ✅ เพิ่มตัวแปร UI ใหม่
    private TextView tvCalendarTotal;
    private TextView tvCalendarIncome;
    private TextView tvCalendarExpense;

    private CalendarView calendarView;
    private RecyclerView recyclerTransactions;

    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private String selectedDate;

    private ExecutorService executor;
    private Handler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        transactionList = new ArrayList<>();

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        tvPoints = view.findViewById(R.id.tv_points_calendar);

        // ✅ เชื่อมต่อ View ใหม่
        tvCalendarTotal = view.findViewById(R.id.tv_calendar_total);
        tvCalendarIncome = view.findViewById(R.id.tv_calendar_income);
        tvCalendarExpense = view.findViewById(R.id.tv_calendar_expense);

        calendarView = view.findViewById(R.id.calendar_view);
        recyclerTransactions = view.findViewById(R.id.recycler_transactions_calendar);

        recyclerTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        recyclerTransactions.setAdapter(transactionAdapter);

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        setupCalendarListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            refreshData();
        });
    }

    private void refreshData() {
        executor.execute(() -> {
            int currentPoints = gamificationLogic.performDailyCheckIn();
            CalendarData data = loadDataForSelectedDate();

            handler.post(() -> {
                if (getContext() == null) return;

                tvPoints.setText(String.valueOf(currentPoints));

                // ✅ อัปเดต UI ทั้ง 3 ยอด
                tvCalendarTotal.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", data.netTotal));
                tvCalendarIncome.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", data.incomeTotal));
                tvCalendarExpense.setText("฿ " + String.format(Locale.getDefault(), "%,.2f", Math.abs(data.expenseTotal))); // ใช้ Math.abs ให้แสดงเป็นบวกสวยๆ

                transactionList.clear();
                transactionList.addAll(data.newList);
                transactionAdapter.notifyDataSetChanged();
            });
        });
    }

    private CalendarData loadDataForSelectedDate() {
        if (getContext() == null) {
            return new CalendarData(0, 0, 0, new ArrayList<>());
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Transaction> newList = new ArrayList<>();
        double incomeTotal = 0;
        double expenseTotal = 0;

        // ✅ Query 1: หา Income ของวันนั้น (Price > 0)
        Cursor incomeCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE DATE(" + DatabaseHelper.COL_TR_TIMESTAMP + ") = ? AND " + DatabaseHelper.COL_TR_PRICE + " > 0",
                new String[]{selectedDate}
        );
        if (incomeCursor.moveToFirst()) {
            incomeTotal = incomeCursor.getDouble(0);
        }
        incomeCursor.close();

        // ✅ Query 2: หา Expense ของวันนั้น (Price < 0)
        Cursor expenseCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE DATE(" + DatabaseHelper.COL_TR_TIMESTAMP + ") = ? AND " + DatabaseHelper.COL_TR_PRICE + " < 0",
                new String[]{selectedDate}
        );
        if (expenseCursor.moveToFirst()) {
            expenseTotal = expenseCursor.getDouble(0);
        }
        expenseCursor.close();

        // คำนวณ Net Total
        double netTotal = incomeTotal + expenseTotal;

        // Query 3: ลิสต์รายการ (เหมือนเดิม)
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
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
            String categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));

            newList.add(new Transaction(id, name, price, timestamp, categoryId, categoryName, categoryIconName));
        }
        cursor.close();

        return new CalendarData(incomeTotal, expenseTotal, netTotal, newList);
    }

    // ✅ ปรับปรุง Class Helper ให้เก็บ 3 ค่า
    private static class CalendarData {
        final double incomeTotal;
        final double expenseTotal;
        final double netTotal;
        final List<Transaction> newList;

        CalendarData(double incomeTotal, double expenseTotal, double netTotal, List<Transaction> newList) {
            this.incomeTotal = incomeTotal;
            this.expenseTotal = expenseTotal;
            this.netTotal = netTotal;
            this.newList = newList;
        }
    }
}