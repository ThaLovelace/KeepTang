package com.example.keeptang.ui.charts; // (Package 'ui.charts' ของคุณ)

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler; // ✅ "Bug Fix" (แก้ ANR)
import android.os.Looper;  // ✅ "Bug Fix" (แก้ ANR)
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keeptang.R;
import com.example.keeptang.data.ChartData; // (Import "โมเดล" (Model) ที่เพิ่งสร้าง)
import com.example.keeptang.data.DatabaseHelper; // (Import 'data')
import com.example.keeptang.logic.Gamification; // (Import 'logic')

// (Import "Library กราฟ" (Chart Library) ... (ที่ "Sync" (Sync) "ผ่าน" (Passed) แล้ว))
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService; // ✅ "Bug Fix" (แก้ ANR)
import java.util.concurrent.Executors; // ✅ "Bug Fix" (แก้ ANR)

public class ChartsFragment extends Fragment {

    // 1. "ประกาศ" (Declare) "View" (UI)
    private TextView tvPoints;
    private RadioGroup toggleGroupCharts;
    private PieChart pieChart;
    private RecyclerView recyclerChartSummary;

    // 2. "ประกาศ" (Declare) "Logic" (Logic)
    private Gamification gamificationLogic;
    private DatabaseHelper dbHelper;
    private ChartSummaryAdapter summaryAdapter;
    private List<ChartData> chartDataList;
    private List<Integer> chartColors;

    // ✅ "Bug Fix" (แก้ ANR): "สร้าง" (Create) "เครื่องมือ" (Tools) "เธรด" (Threading)
    private ExecutorService executor;
    private Handler handler;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        // 4. "เชื่อม" (Connect) "Logic" (Logic)
        dbHelper = new DatabaseHelper(getContext());
        gamificationLogic = new Gamification(getContext());
        chartDataList = new ArrayList<>();

        // ✅ "Bug Fix" (แก้ ANR): "สร้าง" (Initialize) "เครื่องมือ" (Tools) "เธรด" (Threading)
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        setupChartColors();

        // 5. "เชื่อม" (Connect) "View" (UI)
        tvPoints = view.findViewById(R.id.tv_points_chart);
        toggleGroupCharts = view.findViewById(R.id.toggle_group_charts);
        pieChart = view.findViewById(R.id.pie_chart);
        recyclerChartSummary = view.findViewById(R.id.recycler_chart_summary);

        // 6. "ตั้งค่า" (Setup) "RecyclerView" (RecyclerView) (ลิสต์ 3 คอลัมน์)
        recyclerChartSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        summaryAdapter = new ChartSummaryAdapter(getContext(), chartDataList, chartColors);
        recyclerChartSummary.setAdapter(summaryAdapter);

        // 7. "ติดตั้ง" (Setup) "ตัวดักฟัง" (Listener) "ปุ่ม" (Buttons)
        setupToggleButtons();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // ✅ "Bug Fix" (แก้ ANR): "ส่ง" (Execute) "งานหนัก" (Heavy work) "เบื้องหลัง" (Background)
        executor.execute(() -> {
            // --- (รัน "เบื้องหลัง" (Background)) ---
            int currentPoints = gamificationLogic.performDailyCheckIn();
            ChartResultData data = loadChartData(false); // (Default = "Paid")

            // "ส่ง" (Send) "กลับ" (Back) "เธรด UI" (UI Thread)
            handler.post(() -> {
                // --- (รัน "หน้าบ้าน" (UI Thread)) ---
                tvPoints.setText(String.valueOf(currentPoints));

                if (data != null) {
                    pieChart.setVisibility(View.VISIBLE);
                    recyclerChartSummary.setVisibility(View.VISIBLE);
                    setupPieChart(data.pieEntries);
                    chartDataList.clear();
                    chartDataList.addAll(data.summaryList);
                    summaryAdapter.notifyDataSetChanged();
                } else {
                    // (ถ้า "ไม่มี" (No) "ข้อมูล" (Data)... "ซ่อน" (Hide) "ทุกอย่าง" (Everything))
                    pieChart.clear();
                    chartDataList.clear();
                    summaryAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void setupToggleButtons() {
        toggleGroupCharts.setOnCheckedChangeListener((group, checkedId) -> {

            // ✅ "Bug Fix" (แก้ ANR): "ส่ง" (Execute) "งานหนัก" (Heavy work) "เบื้องหลัง" (Background)
            executor.execute(() -> {
                // --- (รัน "เบื้องหลัง" (Background)) ---
                ChartResultData data = null;

                if (checkedId == R.id.toggle_paid) {
                    data = loadChartData(false); // (false = "ไม่" (Not) ใช่ "Income")
                } else if (checkedId == R.id.toggle_income) {
                    data = loadChartData(true); // (true = "ใช่" (Yes) "Income")
                }

                // (เรา "ต้อง" (Must) "ส่ง" (Pass) 'data' (Data) "ตัวนี้" (This)...
                // ... (ที่เป็น 'final' (Final))... "เข้าไป" (Into) "ข้างใน" (Inside) 'handler.post')
                final ChartResultData finalData = data;

                // "ส่ง" (Send) "กลับ" (Back) "เธรด UI" (UI Thread)
                handler.post(() -> {
                    // --- (รัน "หน้าบ้าน" (UI Thread)) ---
                    if (checkedId == R.id.toggle_trend) {
                        // (MVP 6 วัน: "เว้นว่างไว้")
                        pieChart.setVisibility(View.GONE);
                        recyclerChartSummary.setVisibility(View.GONE);
                    } else if (finalData != null) {
                        // (ถ้ากด "Paid" หรือ "Income" ... และ "มี" (Have) "ข้อมูล" (Data))
                        pieChart.setVisibility(View.VISIBLE);
                        recyclerChartSummary.setVisibility(View.VISIBLE);
                        setupPieChart(finalData.pieEntries);
                        chartDataList.clear();
                        chartDataList.addAll(finalData.summaryList);
                        summaryAdapter.notifyDataSetChanged();
                    } else {
                        // (ถ้า "ไม่มี" (No) "ข้อมูล" (Data)... "ซ่อน" (Hide) "ทุกอย่าง" (Everything))
                        pieChart.clear();
                        chartDataList.clear();
                        summaryAdapter.notifyDataSetChanged();
                    }
                });
            });
        });
    }

    /**
     * "โหลด" (Load) "ข้อมูล" (Data) "สรุปยอด" (Summary)
     * (*** "ห้าม" (MUST NOT) "เรียก" (Call) "เมธอด" (Method) นี้ "ตรงๆ" (Directly) "จาก 'onResume'" (From 'onResume') ***)
     */
    private ChartResultData loadChartData(boolean isIncome) {
        if (getContext() == null) return null; // (ป้องกัน "Crash" (Crash))

        List<ChartData> newSummaryList = new ArrayList<>();
        ArrayList<PieEntry> newPieEntries = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        double totalAmount = 0;
        String sign = isIncome ? ">" : "<";
        Cursor totalCursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_TR_PRICE + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COL_TR_PRICE + " " + sign + " 0", null
        );
        if (totalCursor.moveToFirst()) {
            totalAmount = totalCursor.getDouble(0);
        }
        totalCursor.close();

        if (totalAmount == 0) {
            return null; // (คืนค่า "Null" (Null) ... ถ้า "ไม่มี" (No) "ข้อมูล" (Data))
        }

        String query = "SELECT C." + DatabaseHelper.COL_CAT_NAME + ", C." + DatabaseHelper.COL_CAT_ICON + ", SUM(T." + DatabaseHelper.COL_TR_PRICE + ") as CategorySum" +
                " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " T" +
                " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " C ON T." + DatabaseHelper.COL_TR_CATEGORY_ID + " = C." + DatabaseHelper.COL_CAT_ID +
                " WHERE T." + DatabaseHelper.COL_TR_PRICE + " " + sign + " 0" +
                " GROUP BY T." + DatabaseHelper.COL_TR_CATEGORY_ID +
                " ORDER BY CategorySum " + (isIncome ? "DESC" : "ASC");

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
            String categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("CategorySum"));
            double percentage = (Math.abs(amount) / Math.abs(totalAmount)) * 100;

            // "เพิ่ม" (Add) "ข้อมูล" (Data) ... ลงใน "List" (List) (สำหรับ "RecyclerView")
            newSummaryList.add(new ChartData(categoryName, percentage, amount, categoryIconName));

            // "เพิ่ม" (Add) "ข้อมูล" (Data) ... ลงใน "Pie Chart" (Pie Chart)
            newPieEntries.add(new PieEntry((float) Math.abs(amount), categoryName));
        }
        cursor.close();

        // "คืนค่า" (Return) "ผลลัพธ์" (Result) (2 อย่าง... ใน "ห่อ" (Wrapper))
        return new ChartResultData(newSummaryList, newPieEntries);
    }

    /**
     * "ตั้งค่า" (Setup) "Pie Chart" (Pie Chart) (ด้วย MPAndroidChart)
     */
    private void setupPieChart(ArrayList<PieEntry> entries) {
        if (pieChart == null) return; // (ป้องกัน "Crash" (Crash) ถ้า "View" (View) "ยัง" (Not yet) "พร้อม" (Ready))

        PieDataSet dataSet = new PieDataSet(entries, "Expenses");
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
    }

    /**
     * "สร้าง" (Create) "ชุดสี" (Color Palette) (สำหรับ "Chart" (Chart) และ "Legend" (Legend))
     */
    private void setupChartColors() {
        chartColors = new ArrayList<>();
        if (getContext() == null) return; // (ป้องกัน "Crash" (Crash))

        chartColors.add(ContextCompat.getColor(getContext(), R.color.colorCardBlue));
        chartColors.add(ContextCompat.getColor(getContext(), R.color.yellow_tint));
        chartColors.add(ContextCompat.getColor(getContext(), R.color.red));
        chartColors.add(ContextCompat.getColor(getContext(), R.color.yellow));
        // (เพิ่มสีอื่นๆ... ให้ "พอ" (Enough) กับ "11 หมวดหมู่" ของเรา)
        chartColors.add(Color.parseColor("#FFC107"));
        chartColors.add(Color.parseColor("#FF5722"));
        chartColors.add(Color.parseColor("#4CAF50"));
        chartColors.add(Color.parseColor("#03A9F4"));
        chartColors.add(Color.parseColor("#9C27B0"));
        chartColors.add(Color.parseColor("#E91E63"));
        chartColors.add(ContextCompat.getColor(getContext(), R.color.gray));
    }

    /**
     * "ผู้ช่วย" (Helper) "Class" (Class) "จิ๋ว" (Tiny) ...
     * ... (ใช้ "ห่อ" (Wrap) "ผลลัพธ์" (Result) 2 ค่า... จาก "Background Thread" (Background Thread))
     */
    private static class ChartResultData {
        final List<ChartData> summaryList;
        final ArrayList<PieEntry> pieEntries;

        ChartResultData(List<ChartData> summaryList, ArrayList<PieEntry> pieEntries) {
            this.summaryList = summaryList;
            this.pieEntries = pieEntries;
        }
    }
}