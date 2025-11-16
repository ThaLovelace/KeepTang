package com.example.keeptang.ui.charts; // (Package 'ui.charts' (ถ้าคุณสร้าง))

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keeptang.R;
import com.example.keeptang.data.ChartData; // (Import "โมเดล" (Model) ที่เพิ่งสร้าง)

import java.util.List;
import java.util.Locale;

// (นี่คือ "Adapter" ที่ "คุม" (Controls) 'list_item_chart_summary.xml')
public class ChartSummaryAdapter extends RecyclerView.Adapter<ChartSummaryAdapter.ChartViewHolder> {

    private Context context;
    private List<ChartData> chartDataList; // "ข้อมูล" (Data) สรุป
    private List<Integer> chartColors; // "สี" (Colors) ของ Pie Chart (ที่เราจะ "ส่ง" (Pass) มา)

    // 1. Constructor (ตัวสร้าง)
    public ChartSummaryAdapter(Context context, List<ChartData> chartDataList, List<Integer> chartColors) {
        this.context = context;
        this.chartDataList = chartDataList;
        this.chartColors = chartColors;
    }

    // 2. "สร้าง" (Create) "ช่อง" (View Holder)
    @NonNull
    @Override
    public ChartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "โหลด" (Inflate) "ไส้ใน 3 คอลัมน์" (3-Column Item) 'list_item_chart_summary.xml'
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_chart_summary, parent, false);
        return new ChartViewHolder(view);
    }

    // 3. "ยัด" (Bind) "ข้อมูล" (Data)
    @Override
    public void onBindViewHolder(@NonNull ChartViewHolder holder, int position) {
        // "ดึง" (Get) "ข้อมูลสรุป" (Summary Data) ทีละอัน...
        ChartData data = chartDataList.get(position);

        // --- "ยัด" (Bind) "ข้อมูล" (Data) ลง "View" (View) (3 คอลัมน์) ---

        // (ยัด "ชื่อประเภท" (Category Name))
        holder.tvItemName.setText(data.getCategoryName());

        // (ยัด "เปอร์เซ็นต์" (Percentage))
        holder.tvItemPercent.setText(String.format(Locale.getDefault(), "%.0f%%", data.getPercentage()));

        // (ยัด "จำนวนเงิน" (Amount))
        holder.tvItemAmount.setText(String.format(Locale.getDefault(), "฿ %,.2f", Math.abs(data.getAmount())));

        // (ยัด "สี" (Color)!)
        // (นี่คือ "Logic" (Logic) ที่ "เปลี่ยนสี" (Change color) "วงกลม" (Circle View))
        if (position < chartColors.size()) {
            GradientDrawable circle = (GradientDrawable) holder.vItemColorCircle.getBackground();
            circle.setColor(chartColors.get(position)); // (เซ็ต "สี" (Color) ให้ "ตรง" (Match) กับ Pie Chart)
        }
    }

    @Override
    public int getItemCount() {
        return chartDataList.size();
    }


    // "ตัวจับ" (View Holder) (ที่ "เชื่อม" (Connect) XML กับ Java)
    public static class ChartViewHolder extends RecyclerView.ViewHolder {
        View vItemColorCircle; // "วงกลมสี" (Color Circle)
        TextView tvItemName;
        TextView tvItemPercent;
        TextView tvItemAmount;

        public ChartViewHolder(@NonNull View itemView) {
            super(itemView);
            // "เชื่อม" (Connect) "ไส้ใน" (Item) ('list_item_chart_summary.xml')
            vItemColorCircle = itemView.findViewById(R.id.chart_item_color_circle);
            tvItemName = itemView.findViewById(R.id.chart_item_name);
            tvItemPercent = itemView.findViewById(R.id.chart_item_percent);
            tvItemAmount = itemView.findViewById(R.id.chart_item_amount);
        }
    }
}