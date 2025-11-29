package com.example.keeptang.ui.calendar;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private LinearLayout btnPointsContainer;

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

        tvPoints = view.findViewById(R.id.tv_points);
        btnPointsContainer = view.findViewById(R.id.btn_points_container);

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
        setupPointsClickListener();

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

    private void setupPointsClickListener() {
        btnPointsContainer.setOnClickListener(v -> {
            Gamification.CheckInResult checkInResult = gamificationLogic.performDailyCheckIn();
            showCheckInDialog(checkInResult.pointsEarned, checkInResult.streak);
        });
    }

    private void refreshData() {
        executor.execute(() -> {
            Gamification.CheckInResult currentPoints = gamificationLogic.performDailyCheckIn();
            CalendarData data = loadDataForSelectedDate();

            handler.post(() -> {
                if (getContext() == null) return;

                tvPoints.setText(String.valueOf(currentPoints.totalPoints));

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

    private void showCheckInDialog(int pointsEarned, int streak) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_checkin, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogPoints = dialogView.findViewById(R.id.tv_checkin_points);
        Button btnCollect = dialogView.findViewById(R.id.btn_collect);

        tvDialogPoints.setText("+" + pointsEarned + " Points");

        for (int i = 1; i <= 7; i++) {
            int resId = getResources().getIdentifier("day_" + i, "id", getContext().getPackageName());
            View dayView = dialogView.findViewById(resId);

            if (dayView != null) {
                TextView tvDayPoints = dayView.findViewById(R.id.tv_day_points);
                ImageView ivIcon = dayView.findViewById(R.id.iv_day_icon);

                int dayPoints = gamificationLogic.getPointsForDay(i);
                tvDayPoints.setText("+" + dayPoints);

                if (i == 5) ivIcon.setImageResource(R.drawable.icon_10point);
                else if (i == 7) ivIcon.setImageResource(R.drawable.icon_15point);
                else ivIcon.setImageResource(R.drawable.icon_5point);

                if (i < streak) {
                    dayView.setAlpha(0.5f);
                    ivIcon.setImageResource(R.drawable.icon_check);
                } else if (i == streak) {
                    dayView.setBackgroundResource(R.drawable.button_selector_yellow);
                    dayView.setAlpha(1.0f);
                } else {
                    dayView.setBackgroundResource(R.drawable.rounded_edittext_white);
                    dayView.setAlpha(1.0f);
                }
            }
        }

        btnCollect.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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