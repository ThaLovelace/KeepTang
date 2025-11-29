package com.example.keeptang.ui.home;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.example.keeptang.ui.input.AddTransactionFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView tvPoints;
    private LinearLayout btnPointsContainer;
    private TextView tvTotalBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private RecyclerView recyclerTransactions;
    private Spinner spinnerFilter;

    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;

    private ExecutorService executor;
    private Handler handler;

    private String startDateStr = "";
    private String endDateStr = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        transactionList = new ArrayList<>();

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        tvPoints = view.findViewById(R.id.tv_points);
        btnPointsContainer = view.findViewById(R.id.btn_points_container);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        recyclerTransactions = view.findViewById(R.id.recycler_transactions);
        spinnerFilter = view.findViewById(R.id.spinner_filter);

        recyclerTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        transactionAdapter = new TransactionAdapter(getContext(), transactionList, transaction -> {
            AddTransactionFragment editFragment = AddTransactionFragment.newInstance(
                    transaction.getId(),
                    transaction.getName(),
                    transaction.getPrice(),
                    transaction.getCategoryId(),
                    transaction.getTimestamp()
            );

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_main, editFragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerTransactions.setAdapter(transactionAdapter);

        setupFilterSpinner();
        setupPointsClickListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void setupFilterSpinner() {
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculateDateRange(position);
                refreshData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupPointsClickListener() {
        btnPointsContainer.setOnClickListener(v -> {
            Gamification.CheckInResult checkInResult = gamificationLogic.performDailyCheckIn();
            showCheckInDialog(checkInResult.pointsEarned, checkInResult.streak);
        });
    }

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
        } else { // Custom/All
            startDateStr = "";
            endDateStr = "";
        }
    }

    private void refreshData() {
        executor.execute(() -> {
            Gamification.CheckInResult checkInResult = gamificationLogic.performDailyCheckIn();
            SummaryData summary = loadSummaryData();
            List<Transaction> recentTransactions = loadRecentTransactions();

            handler.post(() -> {
                if (getContext() == null) return;

                tvPoints.setText(String.valueOf(checkInResult.totalPoints));

                if (checkInResult.isNewCheckIn) {
                    showCheckInDialog(checkInResult.pointsEarned, checkInResult.streak);
                }

                tvTotalBalance.setText(getString(R.string.total_balance, summary.totalBalance));
                tvTotalIncome.setText(getString(R.string.total_income, summary.totalIncome));
                tvTotalExpense.setText(getString(R.string.total_expense, Math.abs(summary.totalExpense)));

                transactionList.clear();
                transactionList.addAll(recentTransactions);
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

        tvDialogPoints.setText(getString(R.string.checkin_points_earned_format, pointsEarned));

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


    private SummaryData loadSummaryData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double totalIncome = 0;
        double totalExpense = 0;

        String dateCondition = "";
        String[] args = null;
        if (!startDateStr.isEmpty()) {
            dateCondition = " AND " + DatabaseHelper.COL_TR_TIMESTAMP + " BETWEEN ? AND ?";
            args = new String[]{startDateStr, endDateStr};
        }

        Cursor incomeCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COL_TR_PRICE + " > 0" + dateCondition, args);
        if (incomeCursor.moveToFirst()) totalIncome = incomeCursor.getDouble(0);
        incomeCursor.close();

        Cursor expenseCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COL_TR_PRICE + " < 0" + dateCondition, args);
        if (expenseCursor.moveToFirst()) totalExpense = expenseCursor.getDouble(0);
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

        String query = "SELECT T.*, C." + DatabaseHelper.COL_CAT_NAME + " AS category_name, C." + DatabaseHelper.COL_CAT_ICON +
                " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " T" +
                " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " C ON T." + DatabaseHelper.COL_TR_CATEGORY_ID + " = C." + DatabaseHelper.COL_CAT_ID +
                whereClause +
                " ORDER BY T." + DatabaseHelper.COL_TR_TIMESTAMP + " DESC" +
                " LIMIT 20";

        Cursor cursor = db.rawQuery(query, args);

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