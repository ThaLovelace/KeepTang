package com.example.keeptang.ui.charts;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keeptang.R;
import com.example.keeptang.data.ChartData;
import com.example.keeptang.data.DatabaseHelper;
import com.example.keeptang.logic.Gamification;
import com.example.keeptang.logic.SmartCategorizer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChartsFragment extends Fragment {

    private TextView tvPoints;
    private LinearLayout btnPointsContainer;
    private TextView tvChartDesc;
    private TextView tvAiComment;
    private RadioGroup toggleGroupCharts;

    private PieChart pieChart;
    private LineChart lineChart;

    private CardView cardSummaryList;
    private CardView cardAiAdvice;

    private RecyclerView recyclerChartSummary;

    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;
    private ChartSummaryAdapter summaryAdapter;
    private List<ChartData> chartDataList;
    private List<Integer> chartColors;

    private ExecutorService executor;
    private Handler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        chartDataList = new ArrayList<>();

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        setupChartColors();

        tvPoints = view.findViewById(R.id.tv_points);
        btnPointsContainer = view.findViewById(R.id.btn_points_container);
        tvChartDesc = view.findViewById(R.id.tv_chart_desc);
        tvAiComment = view.findViewById(R.id.tv_ai_comment);

        toggleGroupCharts = view.findViewById(R.id.toggle_group_charts);

        pieChart = view.findViewById(R.id.pie_chart);
        lineChart = view.findViewById(R.id.line_chart);

        cardSummaryList = view.findViewById(R.id.card_summary_list);
        cardAiAdvice = view.findViewById(R.id.card_ai_advice);

        recyclerChartSummary = view.findViewById(R.id.recycler_chart_summary);

        recyclerChartSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        summaryAdapter = new ChartSummaryAdapter(getContext(), chartDataList, chartColors);
        recyclerChartSummary.setAdapter(summaryAdapter);

        setupToggleButtons();
        setupPointsClickListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshChart(false, false); // Default: Paid
    }

    private void setupToggleButtons() {
        toggleGroupCharts.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.toggle_paid) {
                refreshChart(false, false);
            } else if (checkedId == R.id.toggle_income) {
                refreshChart(true, false);
            } else if (checkedId == R.id.toggle_trend) {
                refreshChart(false, true);
            }
        });
    }

    private void setupPointsClickListener() {
        btnPointsContainer.setOnClickListener(v -> {
            Gamification.CheckInResult checkInResult = gamificationLogic.performDailyCheckIn();
            showCheckInDialog(checkInResult.pointsEarned, checkInResult.streak);
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

    private void refreshChart(boolean isIncome, boolean isTrend) {
        executor.execute(() -> {
            Gamification.CheckInResult currentPoints = gamificationLogic.performDailyCheckIn();

            // 1. เตรียมข้อมูลให้ AI
            double aiTotalIncome = 0;
            double aiTotalExpense = 0;
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor c1 = db.rawQuery("SELECT SUM(price) FROM TRANSACTIONS WHERE price > 0", null);
            if(c1.moveToFirst()) aiTotalIncome = c1.getDouble(0);
            c1.close();

            Cursor c2 = db.rawQuery("SELECT SUM(price) FROM TRANSACTIONS WHERE price < 0", null);
            if(c2.moveToFirst()) aiTotalExpense = Math.abs(c2.getDouble(0));
            c2.close();

            // 2. เรียก AI
            if (isTrend) {
                SmartCategorizer.analyzeFinancialHealth(getContext(), aiTotalIncome, aiTotalExpense, (level, advice) -> {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            cardAiAdvice.setVisibility(View.VISIBLE);
                            tvAiComment.setText(advice);

                            // ✅✅✅ ส่วนที่เพิ่ม: Logic เปลี่ยนสีและไอคอน ✅✅✅

                            // 1. เชื่อมตัวแปรไอคอน
                            android.widget.ImageView ivIcon = cardAiAdvice.findViewById(R.id.iv_ai_icon);

                            int colorRes;
                            int iconRes; // (สมมติว่าคุณมีรูป icon_level0, 1, 2 แล้ว)

                            if (level == 0) {
                                // 🔴 เสี่ยงสูง (พื้นหลังแดงอ่อน / ไอคอนตกใจ)
                                colorRes = Color.parseColor("#FFEBEE");
                                iconRes = R.drawable.icon_level0; // ⚠️ เปลี่ยนเป็น R.drawable.icon_level0 ถ้ามี
                            } else if (level == 1) {
                                // 🟡 ปานกลาง (พื้นหลังส้มอ่อน / ไอคอนเฉยๆ)
                                colorRes = Color.parseColor("#FFF3E0");
                                iconRes = R.drawable.icon_level1; // ⚠️ เปลี่ยนเป็น R.drawable.icon_level1 ถ้ามี
                            } else {
                                // 🟢 ดีมาก (พื้นหลังเขียวอ่อน / ไอคอนยิ้ม)
                                colorRes = Color.parseColor("#E8F5E9");
                                iconRes = R.drawable.icon_level2; // ⚠️ เปลี่ยนเป็น R.drawable.icon_level2 ถ้ามี
                            }

                            // 2. สั่งเปลี่ยนสีและรูป
                            cardAiAdvice.setCardBackgroundColor(colorRes);
                            ivIcon.setImageResource(iconRes);
                        });
                    }
                });
            }

            // 3. เตรียมข้อมูลกราฟ
            ChartResultData pieData = null;
            LineData lineData = null;

            if (isTrend) {
                lineData = loadTrendData(); // โหลดกราฟเส้น 2 เส้น
            } else {
                pieData = loadPieChartData(isIncome);
            }

            final ChartResultData finalPieData = pieData;
            final LineData finalLineData = lineData;

            handler.post(() -> {
                if (getContext() == null) return;
                tvPoints.setText(String.valueOf(currentPoints.totalPoints));

                if (isTrend) {
                    // --- Trend Mode ---
                    pieChart.setVisibility(View.GONE);
                    cardSummaryList.setVisibility(View.GONE);
                    lineChart.setVisibility(View.VISIBLE);

                    // เปลี่ยนชื่อกราฟ
                    tvChartDesc.setText("Income vs Expense Trend (Last 7 Days)");

                    if (finalLineData != null) {
                        setupLineChart(finalLineData);
                    } else {
                        lineChart.clear();
                    }
                } else {
                    // --- Pie Mode ---
                    lineChart.setVisibility(View.GONE);
                    pieChart.setVisibility(View.VISIBLE);
                    cardSummaryList.setVisibility(View.VISIBLE);

                    tvChartDesc.setText(isIncome ? "Income Distribution" : "Expense Distribution");

                    if (finalPieData != null) {
                        setupPieChart(finalPieData.pieEntries);
                        chartDataList.clear();
                        chartDataList.addAll(finalPieData.summaryList);
                        summaryAdapter.notifyDataSetChanged();
                    } else {
                        pieChart.clear();
                        chartDataList.clear();
                        summaryAdapter.notifyDataSetChanged();
                    }
                }
            });
        });
    }

    // ✅✅✅ โหลดข้อมูลกราฟเส้นแบบ 2 เส้น (รายรับ vs รายจ่าย) ✅✅✅
    private LineData loadTrendData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Entry> incomeEntries = new ArrayList<>();
        ArrayList<Entry> expenseEntries = new ArrayList<>();
        final ArrayList<String> labels = new ArrayList<>();

        // Query ยอดรวมรายวัน แยกตามประเภท
        String query = "SELECT DATE(" + DatabaseHelper.COL_TR_TIMESTAMP + ") as tr_date, " +
                "SUM(CASE WHEN " + DatabaseHelper.COL_TR_PRICE + " > 0 THEN " + DatabaseHelper.COL_TR_PRICE + " ELSE 0 END) as daily_income, " +
                "SUM(CASE WHEN " + DatabaseHelper.COL_TR_PRICE + " < 0 THEN ABS(" + DatabaseHelper.COL_TR_PRICE + ") ELSE 0 END) as daily_expense " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "GROUP BY tr_date " +
                "ORDER BY tr_date ASC " +
                "LIMIT 7";

        Cursor cursor = db.rawQuery(query, null);
        int index = 0;

        while (cursor.moveToNext()) {
            String dateStr = cursor.getString(0);
            float dailyIncome = cursor.getFloat(1);
            float dailyExpense = cursor.getFloat(2);

            incomeEntries.add(new Entry(index, dailyIncome));
            expenseEntries.add(new Entry(index, dailyExpense));

            try {
                SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat sdfOut = new SimpleDateFormat("d MMM", Locale.US);
                Date date = sdfIn.parse(dateStr);
                labels.add(sdfOut.format(date));
            } catch (Exception e) {
                labels.add(dateStr);
            }
            index++;
        }
        cursor.close();

        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) return null;

        // เส้นที่ 1: Income (สีเขียว)
        LineDataSet setIncome = new LineDataSet(incomeEntries, "Income");
        setIncome.setColor(Color.parseColor("#4CAF50"));
        setIncome.setLineWidth(3f);
        setIncome.setCircleColor(Color.parseColor("#4CAF50"));
        setIncome.setCircleRadius(5f);
        setIncome.setDrawValues(true);
        setIncome.setValueTextSize(10f);

        // เส้นที่ 2: Expense (สีแดง)
        LineDataSet setExpense = new LineDataSet(expenseEntries, "Expense");
        setExpense.setColor(Color.parseColor("#F44336"));
        setExpense.setLineWidth(3f);
        setExpense.setCircleColor(Color.parseColor("#F44336"));
        setExpense.setCircleRadius(5f);
        setExpense.setDrawValues(true);
        setExpense.setValueTextSize(10f);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setIncome);
        dataSets.add(setExpense);

        LineData lineData = new LineData(dataSets);
        lineChart.setTag(labels); // ฝาก Labels ไว้

        return lineData;
    }

    private void setupLineChart(LineData lineData) {
        if (lineChart == null) return;

        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);

        // ✅✅✅ เปิด Legend เพื่อบอกว่าเส้นไหนคืออะไร ✅✅✅
        Legend l = lineChart.getLegend();
        l.setEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setYOffset(10f);
        l.setTextSize(12f);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setExtraOffsets(10, 10, 10, 20);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        if (lineChart.getTag() instanceof List) {
            List<String> labels = (List<String>) lineChart.getTag();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setLabelCount(labels.size());
        }

        lineChart.getAxisRight().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private ChartResultData loadPieChartData(boolean isIncome) {
        if (getContext() == null) return null;
        List<ChartData> newSummaryList = new ArrayList<>();
        ArrayList<PieEntry> newPieEntries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double totalAmount = 0;
        String sign = isIncome ? ">" : "<";
        Cursor totalCursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " WHERE " + DatabaseHelper.COL_TR_PRICE + " " + sign + " 0", null);
        if (totalCursor.moveToFirst()) totalAmount = totalCursor.getDouble(0);
        totalCursor.close();
        if (totalAmount == 0) return null;
        String query = "SELECT C." + DatabaseHelper.COL_CAT_NAME + " AS category_name, C." + DatabaseHelper.COL_CAT_ICON + ", SUM(T." + DatabaseHelper.COL_TR_PRICE + ") as CategorySum FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " T INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " C ON T." + DatabaseHelper.COL_TR_CATEGORY_ID + " = C." + DatabaseHelper.COL_CAT_ID + " WHERE T." + DatabaseHelper.COL_TR_PRICE + " " + sign + " 0 GROUP BY T." + DatabaseHelper.COL_TR_CATEGORY_ID + " ORDER BY CategorySum " + (isIncome ? "DESC" : "ASC");
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
            String categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("CategorySum"));
            double percentage = (Math.abs(amount) / Math.abs(totalAmount)) * 100;
            newSummaryList.add(new ChartData(categoryName, percentage, amount, categoryIconName));
            newPieEntries.add(new PieEntry((float) Math.abs(amount), categoryName));
        }
        cursor.close();
        return new ChartResultData(newSummaryList, newPieEntries);
    }

    private void setupPieChart(ArrayList<PieEntry> entries) {
        if (pieChart == null) return;
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(chartColors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupChartColors() {
        chartColors = new ArrayList<>();
        if (getContext() == null) return;
        chartColors.add(ContextCompat.getColor(getContext(), R.color.colorCardBlue));
        chartColors.add(ContextCompat.getColor(getContext(), R.color.yellow_tint));
        chartColors.add(ContextCompat.getColor(getContext(), R.color.red));
        chartColors.add(ContextCompat.getColor(getContext(), R.color.yellow));
        chartColors.add(Color.parseColor("#FF9800"));
        chartColors.add(Color.parseColor("#4CAF50"));
        chartColors.add(Color.parseColor("#9C27B0"));
        chartColors.add(Color.parseColor("#2196F3"));
        chartColors.add(Color.LTGRAY);
    }

    private static class ChartResultData {
        final List<ChartData> summaryList;
        final ArrayList<PieEntry> pieEntries;
        ChartResultData(List<ChartData> summaryList, ArrayList<PieEntry> pieEntries) {
            this.summaryList = summaryList;
            this.pieEntries = pieEntries;
        }
    }
}