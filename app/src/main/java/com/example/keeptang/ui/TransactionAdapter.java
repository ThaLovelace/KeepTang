package com.example.keeptang.ui; // (Package 'ui' ของคุณ)

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.keeptang.R;
import com.example.keeptang.data.Transaction; // (Import "โมเดล" (Model) ที่เพิ่งสร้าง)
import java.util.List;
import java.util.Locale;

// (นี่คือ "Adapter" ที่ "คุม" (Controls) 'list_item_transaction.xml')
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactionList; // "ข้อมูล" (Data) ทั้งหมด

    // 1. Constructor (ตัวสร้าง)
    public TransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    // 2. "สร้าง" (Create) "ช่อง" (View Holder)
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "โหลด" (Inflate) "ไส้ใน" (Item Layout) 'list_item_transaction.xml'
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    // 3. "ยัด" (Bind) "ข้อมูล" (Data)
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        // "ดึง" (Get) "Transaction" (Transaction) ทีละอัน...
        Transaction transaction = transactionList.get(position);

        // --- "ยัด" (Bind) "ข้อมูล" (Data) ลง "View" (View) ---

        // (ยัด "ชื่อ" (Name) และ "ชื่อหมวดหมู่" (Category Name))
        holder.tvItemName.setText(transaction.getName());
        holder.tvItemCategory.setText(transaction.getCategoryName());

        // (ยัด "ไอคอนน่ารัก" (Cute Icon))
        int iconResId = context.getResources().getIdentifier(
                transaction.getCategoryIconName(),
                "drawable",
                context.getPackageName()
        );
        holder.ivItemIcon.setImageResource(iconResId);

        // (ยัด "จำนวนเงิน" (Amount))
        double price = transaction.getPrice();
        holder.tvItemAmount.setText(String.format(Locale.getDefault(), "฿ %,.2f", price));

        // (Logic "เปลี่ยนสี" (Change Color) ... (ถ้า "ติดลบ" (Negative) -> สีแดง ... ถ้า "บวก" (Positive) -> สีดำ))
        if (price < 0) {
            holder.tvItemAmount.setTextColor(ContextCompat.getColor(context, R.color.red)); // (ใช้ @color/red)
        } else {
            holder.tvItemAmount.setTextColor(ContextCompat.getColor(context, R.color.black)); // (ใช้ @color/black)
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }


    // "ตัวจับ" (View Holder) (ที่ "เชื่อม" (Connect) XML กับ Java)
    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemIcon;
        TextView tvItemName;
        TextView tvItemCategory;
        TextView tvItemAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            // "เชื่อม" (Connect) "ไส้ใน" (Item) ('list_item_transaction.xml')
            ivItemIcon = itemView.findViewById(R.id.item_icon);
            tvItemName = itemView.findViewById(R.id.item_name);
            tvItemCategory = itemView.findViewById(R.id.item_category);
            tvItemAmount = itemView.findViewById(R.id.item_amount);
        }
    }
}